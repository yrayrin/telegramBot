package com.telegram.mybot;

import com.telegram.mybot.model.Category;
import com.telegram.mybot.model.RootCategory;
import com.telegram.mybot.model.UserData;
import org.apache.commons.lang.StringUtils;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

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
import static com.telegram.mybot.Action.REMOVE_DATA;
import static com.telegram.mybot.Action.START;
import static com.telegram.mybot.Action.STATS;
import static com.telegram.mybot.Constants.ASK_CHILD_CATEGORY;
import static com.telegram.mybot.Constants.ASK_ENTER_SUM;
import static com.telegram.mybot.Constants.ASK_ROOT_CATEGORY;
import static java.util.stream.Collectors.toList;

public class CostBot extends TelegramLongPollingBot {

    private static final String TOKEN = "527851491:AAF3YrRimC0B5flOuE2-ZL8jACzkY9I03ZY";
    private static final String BOT_NAME = "testbot";
    Integer id = 0;
    private MongoDBServiceImpl mongoDBService;

    public void initDB() {
        mongoDBService = new MongoDBServiceImpl();
        mongoDBService.init();
    }

    public void onUpdateReceived(Update update) {
        UserData userData = null;
        try {
            Message messageFromUpdate = getMessageFromUpdate(update);
            if (messageFromUpdate == null) {
                userData = mongoDBService.getUserData(update.getCallbackQuery().getMessage().getChatId());
                addMessageToHistory(update.getCallbackQuery().getData(), update.getCallbackQuery().getMessage().getChatId());
            } else {
                userData = mongoDBService.getUserData(update.getMessage().getChatId());
                addMessageToHistory(messageFromUpdate.getText(), messageFromUpdate.getChatId());
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
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
                    if (ASK_ROOT_CATEGORY.equals(messages.get(messages.size() - 1))) {
                        handleAddRootCategory(update, userData);
                    } else if (ASK_CHILD_CATEGORY.equals(messages.get(messages.size() - 1))) {
                        handleAddChildCategory(update, userData);
                    } else if (ASK_ENTER_SUM.equals(messages.get(messages.size() - 1))) {
                        handleAddSumCallBack(update);
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
        return TOKEN;
    }

    private void addMessageToHistory(String messageText, Long chatId) throws NoSuchAlgorithmException {
        UserData userData = Optional.ofNullable(mongoDBService.getUserData(chatId)).orElse(new UserData(chatId));
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
            List<String> categories = Arrays.asList(StringUtils.trim(text).split(","));
            List<RootCategory> data = userData.getCategories();
            categories.forEach(category -> data.add(new RootCategory(category, null)));
            userData.setCategories(data);
            try {
                mongoDBService.putUserData(userData);
                message.setText("Корневые категории добавлены.");
                addMessageToHistory( message.getText(), userData.getUserId() );// Sending our message object to user
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
            List<String> categories = Arrays.asList(StringUtils.trim(text).split(","));
            String nameOfRootCategory = userData.getHistoryOfMessages().get(userData.getHistoryOfMessages().size() - 2).split("-")[1];
            RootCategory rootCategory = userData.getCategories().stream().filter(rootCategory1 -> nameOfRootCategory.equals(rootCategory1.getName()))
                    .collect(toList()).get(0);
            categories.forEach(category -> rootCategory.getCategories().add(new Category(category)));
            try {
                mongoDBService.putUserData(userData);
                message.setText("Category was added.");// Sending our message object to user
                execute(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAskToAddRootCategory(Long chatId) {
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chatId)
                .setText(ASK_ROOT_CATEGORY);
        try {
            execute(message); // Sending our message object to user
            addMessageToHistory(message.getText(), chatId);
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
                sendAskToAddRootCategory(userData.getUserId());
                break;
            case ADD_CHILD_CATEGORY:
                createRootCategoryButtons(userData);
                break;
            case REMOVE_DATA:
                removeData(userData);
                break;
            case REMOVE_CATEGORY:
                break;
            case ADD_SUM:
                createChildCategoryButtons(userData);
                break;
            case ADD_SUM_VALUE:
                sendAskAboutAddSum(userData.getUserId());
                break;
            case ADD_CHILD_CATEGORY_VALUE:
                sendAskAboutChildCategories(userData.getUserId());
                break;
            case STATS:
                showStats(userData);
                break;
            default:

                break;
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
            addMessageToHistory(message.getText(), userData.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        showStats(userData);
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
            addMessageToHistory(message.getText(), userData.getUserId());
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
                statsMessageBuilder.append(rootCategory.getName());
                statsMessageBuilder.append(":\n ");
                rootCategory.getCategories().forEach(childCategory -> {
                    statsMessageBuilder.append(childCategory.getName());
                    statsMessageBuilder.append(": ");
                    statsMessageBuilder.append(childCategory.getValue() == null ? 0 : childCategory.getValue());
                    statsMessageBuilder.append(" <i>BYR</i>\n");
                });
            });
            message.setText(statsMessageBuilder.toString());
        }
        message.setParseMode(ParseMode.HTML);
        try {
            execute(message);
            addMessageToHistory(message.getText(), userData.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddSumCallBack(Update update) {
        long chat_id = update.getMessage().getChatId();
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chat_id);
        String text = update.getMessage().getText();
        if (StringUtils.isNotBlank(text)) {
            UserData userData = mongoDBService.getUserData(chat_id);
            String nameOfCategory = userData.getHistoryOfMessages().get(userData.getHistoryOfMessages().size() - 3).split("-")[1];
            userData.getCategories().forEach(
                    rootCategory1 -> {
                        List<Category> categoryList = rootCategory1.getCategories().stream().filter(
                                childCategory -> nameOfCategory.equals(childCategory.getName())).collect(toList());
                        if ( !categoryList.isEmpty() ) {
                            if (categoryList.get(0).getValue() == null) {
                                categoryList.get(0).setValue(text);
                            } else {
                                Double d = Double.parseDouble(categoryList.get(0).getValue().replaceAll(",", "."));
                                categoryList.get(0).setValue(String.valueOf(d + Double.parseDouble(text.replaceAll(",", "."))));
                                return;
                            }
                            message.setText("Данные обновлены\n" + nameOfCategory + ": " + categoryList.get(0).getValue() + " <i>BYR</i>");
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

    private void createChildCategoryButtons(UserData userData) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        userData.getCategories().forEach(category -> {
            category.getCategories().forEach(childCategory -> {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText(childCategory.getName()).setCallbackData("addSumValue-" + childCategory.getName()));
                rows.add(rowInline);
            });
        });
        // Add it to the message
        markupInline.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage().setChatId(userData.getUserId())
                .setText("Выберите категорию для заполнения:").setReplyMarkup(markupInline);
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), userData.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createRootCategoryButtons(UserData userData) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        userData.getCategories().forEach(category -> {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(new InlineKeyboardButton().setText(category.getName()).setCallbackData("addChildCategoryValue-" + category.getName()));
            rows.add(rowInline);
        });
        // Add it to the message
        markupInline.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage().setChatId(userData.getUserId())
                .setText("Выберите категорию:").setReplyMarkup(markupInline);
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), userData.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void sendAskAboutChildCategories(Long chatId) {
        SendMessage sendMessage = new SendMessage().setChatId(chatId)
                .setText(ASK_CHILD_CATEGORY);
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), chatId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAskAboutAddSum(Long chatId) {
        SendMessage sendMessage = new SendMessage().setChatId(chatId)
                .setText(ASK_ENTER_SUM);
        try {
            execute(sendMessage);
            addMessageToHistory(sendMessage.getText(), chatId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private UserData createBaseUser(Message message) {
        UserData userData = new UserData(null);
        Optional.ofNullable(message.getFrom()).map(User::getFirstName).ifPresent(userData::setUserFirstName);
        Optional.ofNullable(message.getFrom()).map(User::getLastName).ifPresent(userData::setUserLastName);
        Optional.ofNullable(message.getChatId()).ifPresent(userData::setUserId);
        return userData;
    }
}
