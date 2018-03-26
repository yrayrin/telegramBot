package com.telegram.mybot;

import com.telegram.mybot.model.UserData;
import org.telegram.telegrambots.api.objects.Message;

import java.security.NoSuchAlgorithmException;

public interface MongoDBServiceApi {
    UserData getUserData( Message message);

    void putUserData(UserData userData) throws NoSuchAlgorithmException;

    void updateHistory(UserData userData) throws NoSuchAlgorithmException;

    void removeCategory(UserData userData, String categoryName) throws NoSuchAlgorithmException;

    void removeData(UserData userData) throws NoSuchAlgorithmException;

    void cleanDatabase();
}
