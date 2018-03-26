package com.telegram.mybot;

import com.telegram.mybot.model.Category;
import com.telegram.mybot.model.RootCategory;
import com.telegram.mybot.model.UserData;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.telegram.mybot.Action.ADD_CHILD_CATEGORY;
import static com.telegram.mybot.Action.ADD_CHILD_CATEGORY_VALUE;
import static com.telegram.mybot.Action.ADD_ROOT_CATEGORY;
import static com.telegram.mybot.Action.ADD_SUM;
import static com.telegram.mybot.Action.ADD_SUM_VALUE;
import static com.telegram.mybot.Action.MENU;
import static com.telegram.mybot.Action.REMOVE_ALL;
import static com.telegram.mybot.Action.REMOVE_CATEGORY;
import static com.telegram.mybot.Action.REMOVE_CATEGORY_VALUE;
import static com.telegram.mybot.Action.REMOVE_DATA;
import static com.telegram.mybot.Action.START;
import static com.telegram.mybot.Action.STATS;
import static com.telegram.mybot.Constants.ASK_CHILD_CATEGORY;
import static com.telegram.mybot.Constants.ASK_ENTER_SUM;
import static com.telegram.mybot.Constants.ASK_ROOT_CATEGORY;
import static com.telegram.mybot.Constants.INVALID_DATA_FORMAT_SUM;
import static java.util.stream.Collectors.toList;

public class CostBot extends TelegramLongPollingBot {

    private static final String BOT_NAME = "testbot";
    private MongoDBServiceImpl mongoDBService;
    private static final String FILE_NAME = "bot-token.txt";

    public void initDB() {
        mongoDBService = new MongoDBServiceImpl();
        mongoDBService.init();
    }

    public void onUpdateReceived(Update update) {
        UserData userData = null;
        try {
            Message messageFromUpdate = getMessageFromUpdate(update);
            if (messageFromUpdate == null) {
                userData = mongoDBService.getUserData(update.getCallbackQuery().getMessage());
                addMessageToHistory(update.getCallbackQuery().getData(), userData);
            } else {
                userData = mongoDBService.getUserData(update.getMessage());
                addMessageToHistory(messageFromUpdate.getText(), userData);
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (userData.getHistoryOfMessages().size() > 50) {
            userData.setHistoryOMessages(userData.getHistoryOfMessages()
                    .subList(userData.getHistoryOfMessages().size() - 5, userData.getHistoryOfMessages().size()));
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();

            switch (message_text) {
                case START: {
                    handleStart(userData);
                    break;
                }
                case MENU: {
                    handleMenu(userData);
                    break;
                }
                case REMOVE_ALL:
                    removeAllCategories(userData);
                    break;
                default:
                    List<String> messages = userData.getHistoryOfMessages();
                    if (ASK_ROOT_CATEGORY.equals(messages.get(messages.size() - 2))) {
                        handleAddRootCategory(update, userData);
                    } else if (ASK_CHILD_CATEGORY.equals(messages.get(messages.size() - 2))) {
                        handleAddChildCategory(update, userData);
                    } else if (ASK_ENTER_SUM.equals(messages.get(messages.size() - 2)) || INVALID_DATA_FORMAT_SUM.equals(messages.get(messages.size() - 2))) {
                        handleAddSumCallBack(update, userData);
                    }
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            try {
                handleCallBackActions(update, userData);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public String getBotUsername() {
        return BOT_NAME;
    }

    public String getBotToken() {
        try {
            return  FileUtils.readFileToString( FileUtils.toFile( getClass().getClassLoader().getResource( FILE_NAME ) ) );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "token";
    }

    private void addMessageToHistory(String messageText, UserData userData) throws NoSuchAlgorithmException {
        userData.getHistoryOfMessages().add(messageText);
        mongoDBService.updateHistory(userData);
    }

    private Message getMessageFromUpdate(Update update) {
        return Optional.ofNullable(update.getMessage()).orElse(null);
    }

    private void handleAddRootCategory(Update update, UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId());
        String text = update.getMessage().getText();
        if (StringUtils.isNotBlank(text)) {
            List<String> categories = Arrays.asList(text.split(","));
            List<RootCategory> data = userData.getCategories();
            categories.forEach(category -> data.add(new RootCategory(category.trim(), null)));
            userData.setCategories(data);
            try {
                mongoDBService.putUserData(userData);
                message.setText("Корневые категории добавлены.");
                addMessageToHistory(message.getText(), userData);// Sending our message object to user
                execute(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAddChildCategory(Update update, UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId());
        String text = update.getMessage().getText();
        if (StringUtils.isNotBlank(text)) {
            List<String> categories = Arrays.asList(text.split(","));
            String nameOfRootCategory = userData.getHistoryOfMessages().get(userData.getHistoryOfMessages().size() - 3).split("-")[1];
            RootCategory rootCategory = userData.getCategories().stream().filter(rootCategory1 -> nameOfRootCategory.equals(rootCategory1.getName()))
                    .collect(toList()).get(0);
            categories.forEach(category -> rootCategory.getCategories().add(new Category(category.trim())));
            try {
                mongoDBService.putUserData(userData);
                message.setText("Category was added.");// Sending our message object to user
                execute(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAskToAddRootCategory(UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId())
                .setText(ASK_ROOT_CATEGORY);
        try {
            execute(message); // Sending our message object to user
            addMessageToHistory(message.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStart(UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId())
                .setText("You are welcome!");
        try {
            execute(message);
            handleMenu(userData);// Sending our message object to user
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMenu(UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId())
                .setText("Меню выбора:");
        message.setReplyMarkup(createMenuButtons());
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleDefault(Update update) {
        long id = update.getCallbackQuery().getMessage().getChatId();
        SendMessage message = new SendMessage()
                .setChatId(id)// Create a message object object
                .setText("You send lol");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallBackActions(Update update, UserData userData) throws NoSuchAlgorithmException {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callBackData = callbackQuery.getData();
        switch (callBackData.split("-")[0]) {
            case ADD_ROOT_CATEGORY:
                sendAskToAddRootCategory(userData);
                break;
            case ADD_CHILD_CATEGORY:
                createRootCategoryButtons(userData);
                break;
            case REMOVE_DATA:
                removeData(userData);
                break;
            case REMOVE_CATEGORY:
                createChildCategoryButtons(userData, true);
                break;
            case ADD_SUM:
                createChildCategoryButtons(userData, false);
                break;
            case ADD_SUM_VALUE:
                sendAskAboutAddSum(userData);
                break;
            case ADD_CHILD_CATEGORY_VALUE:
                sendAskAboutChildCategories(userData);
                break;
            case REMOVE_CATEGORY_VALUE:
                removeCategory(userData, callBackData.split("-")[1]);
                break;
            case STATS:
                showStats(userData);
                break;
            default:

                break;
        }
    }

    private void removeCategory(UserData userData, String categoryName) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId());
        try {
            mongoDBService.removeCategory(userData, categoryName);
            message.setText("Категория удалена");
        } catch (NoSuchAlgorithmException e) {
            message.setText("Error. Shit happens");
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void removeData(UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId());
        userData.getCategories().forEach(rootCategory -> {
            rootCategory.getCategories().forEach(childCategory -> {
                childCategory.setValue(null);
            });
        });
        try {
            mongoDBService.putUserData(userData);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            message.setText("Ошибка обновления, попробуйте выйти в окно");
        }
        try {
            message.setText("Данные обновлены");
            execute(message);
            addMessageToHistory(message.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!userData.getCategories().isEmpty()) {
            showStats(userData);
        }
    }

    public void removeAllCategories(UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId());
        try {
            userData.getHistoryOfMessages().clear();
            userData.getCategories().clear();
            mongoDBService.removeData(userData);
            message.setText("Все твои затраты сейчас в могиле..");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            message.setText("Ошибка удаления, в окне you de way");
        }
        try {
            execute(message);
            addMessageToHistory(message.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStats(UserData userData) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(userData.getUserId());
        if (userData.getCategories().isEmpty()) {
            message.setText("<b>Ну ты и тупица, заполни таблицу для начала!</b>");
        } else {
            StringBuilder statsMessageBuilder = new StringBuilder();
            userData.getCategories().forEach(rootCategory -> {
                if (!rootCategory.getCategories().isEmpty()) {
                    statsMessageBuilder.append("<i>" + rootCategory.getName() + "</i>");
                    statsMessageBuilder.append(":\n");
                    rootCategory.getCategories().forEach(childCategory -> {
                        statsMessageBuilder.append("    ");
                        statsMessageBuilder.append(childCategory.getName());
                        statsMessageBuilder.append(": ");
                        statsMessageBuilder.append(childCategory.getValue() == null ? 0 : childCategory.getValue());
                        statsMessageBuilder.append(" <i>BYR</i>\n");
                    });
                }
            });
            message.setText(statsMessageBuilder.toString());
        }
        message.setParseMode(ParseMode.HTML);
        try {
            execute(message);
            addMessageToHistory(message.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddSumCallBack(Update update, UserData userData) {
        long chat_id = update.getMessage().getChatId();
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chat_id);
        String text = update.getMessage().getText();
        if (StringUtils.isNotBlank(text)) {
            String[] t = userData.getHistoryOfMessages().get(userData.getHistoryOfMessages().size() - 3).split("-");
            String nameOfCategory = t.length == 2 ? t[1] : t[0];
            userData.getCategories().forEach(
                    rootCategory1 -> {
                        List<Category> categoryList = rootCategory1.getCategories().stream().filter(
                                childCategory -> nameOfCategory.equals(childCategory.getName())).collect(toList());
                        if (!categoryList.isEmpty()) {
                            if (categoryList.get(0).getValue() == null) {
                                try {
                                    Double d = Double.parseDouble(text.replaceAll(",", "."));
                                    categoryList.get(0).setValue(String.valueOf(d));
                                    message.setText("Данные обновлены\n" + nameOfCategory + ": " + categoryList.get(0).getValue() + " <i>BYR</i>");
                                } catch (Exception ex) {
                                    message.setText(INVALID_DATA_FORMAT_SUM);
                                    try {
                                        addMessageToHistory(nameOfCategory, userData);
                                        addMessageToHistory(message.getText(), userData);
                                    } catch (NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                Double d = Double.parseDouble(categoryList.get(0).getValue());
                                try {
                                    categoryList.get(0).setValue(String.valueOf(d + Double.parseDouble(text.replaceAll(",", "."))));
                                    message.setText("Данные обновлены\n" + nameOfCategory + ": " + categoryList.get(0).getValue() + " <i>BYR</i>");
                                } catch (NumberFormatException ex1) {
                                    message.setText(INVALID_DATA_FORMAT_SUM);
                                    try {
                                        addMessageToHistory(nameOfCategory, userData);
                                        addMessageToHistory(message.getText(), userData);
                                    } catch (NoSuchAlgorithmException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        }
                    });
            try {
                mongoDBService.putUserData(userData);// Sending our message object to user
                message.setParseMode(ParseMode.HTML);
                execute(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private InlineKeyboardMarkup createMenuButtons() {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(new InlineKeyboardButton().setText("Добавить корневую категорию").setCallbackData(ADD_ROOT_CATEGORY));
        rowInline1.add(new InlineKeyboardButton().setText("Добавить подкатегорию").setCallbackData(ADD_CHILD_CATEGORY));
        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(new InlineKeyboardButton().setText("Обнулить данные").setCallbackData(REMOVE_DATA));
        rowInline2.add(new InlineKeyboardButton().setText("Удалить категорию").setCallbackData(REMOVE_CATEGORY));
        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        rowInline3.add(new InlineKeyboardButton().setText("Добавить сумму").setCallbackData(ADD_SUM));
        rowInline3.add(new InlineKeyboardButton().setText("Статистика").setCallbackData(STATS));
        // Set the keyboard to the markup
        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        rowsInline.add(rowInline3);
        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    private void createChildCategoryButtons(UserData userData, boolean isRemoveOperation) {
        SendMessage sendMessage = new SendMessage().setChatId(userData.getUserId());
        if (!userData.getCategories().isEmpty()) {
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            final String prefix = isRemoveOperation ? "removeCategoryValue-" : "addSumValue-";

            userData.getCategories().forEach(category -> {
                int size = category.getCategories().isEmpty() ? 1 : category.getCategories().size();
                for (int i = 0; i < size; i++) {
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
                    if (i == 0) {
                        rowInline.add(new InlineKeyboardButton().setText(category.getName()).setCallbackData(isRemoveOperation ? prefix + category.getName() : "X"));
                        rowInline.add(new InlineKeyboardButton().setText("X").setCallbackData("X"));
                        if (!category.getCategories().isEmpty()) {
                            rowInline1.add(new InlineKeyboardButton().setText("X").setCallbackData("X"));
                            rowInline1.add(new InlineKeyboardButton().setText(
                                    category.getCategories().get(i).getName()).setCallbackData(prefix + category.getCategories().get(i).getName()));
                        }
                    } else {
                        rowInline.add(new InlineKeyboardButton().setText("X").setCallbackData("X"));
                        rowInline.add(new InlineKeyboardButton().setText(
                                category.getCategories().get(i).getName()).setCallbackData(prefix + category.getCategories().get(i).getName()));
                    }
                    rows.add(rowInline);
                    if (i == 0 && !category.getCategories().isEmpty()) {
                        rows.add(rowInline1);
                    }
                }
            });
            // Add it to the message
            markupInline.setKeyboard(rows);


            sendMessage.setText("Выберите категорию:").setReplyMarkup(markupInline);
        } else {
            sendMessage.setText("Нет данных для удаления. Создай root. It is not de way");
        }
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createRootCategoryButtons(UserData userData) {
        SendMessage sendMessage = new SendMessage().setChatId(userData.getUserId());
        if (!userData.getCategories().isEmpty()) {
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            userData.getCategories().forEach(category -> {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText(category.getName()).setCallbackData("addChildCategoryValue-" + category.getName()));
                rows.add(rowInline);
            });
            // Add it to the message
            markupInline.setKeyboard(rows);

            sendMessage.setText("Выберите категорию:").setReplyMarkup(markupInline);
        } else {
            sendMessage.setText("Нет данных для отображения. Создай root. It is not de way");
        }
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void sendAskAboutChildCategories(UserData userData) {
        SendMessage sendMessage = new SendMessage().setChatId(userData.getUserId())
                .setText(ASK_CHILD_CATEGORY);
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAskAboutAddSum(UserData userData) {
        SendMessage sendMessage = new SendMessage().setChatId(userData.getUserId())
                .setText(ASK_ENTER_SUM);
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), userData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
