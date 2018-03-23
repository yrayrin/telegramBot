package com.telegram.mybot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        CostBot costBot = new CostBot();
        costBot.initDB();
        try {
            botsApi.registerBot(costBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
