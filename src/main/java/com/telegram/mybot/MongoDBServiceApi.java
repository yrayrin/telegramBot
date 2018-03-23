package com.telegram.mybot;

import com.telegram.mybot.model.UserData;

import java.security.NoSuchAlgorithmException;

public interface MongoDBServiceApi {
    UserData getUserData(Long userId);
    void putUserData( UserData userData ) throws NoSuchAlgorithmException;
    void updateHistory( UserData userData ) throws NoSuchAlgorithmException;
    void removeCategory ( String categoryName );
    void removeData( UserData userData ) throws NoSuchAlgorithmException;
    void cleanDatabase();
}
