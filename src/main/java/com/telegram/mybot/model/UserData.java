package com.telegram.mybot.model;

import java.util.ArrayList;
import java.util.List;

public class UserData {

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public List<String> getHistoryOfMessages() {
        if ( messages == null ) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    public void setHistoryOMessages(List<String> messages) {
        this.messages = messages;
    }

    public List<RootCategory> getCategories() {
        if ( categories == null ) {
            categories = new ArrayList<>();
        }
        return categories;
    }

    public void setCategories(List<RootCategory> categories) {
        this.categories = categories;
    }

    public UserData( Long userId ) {
        this.userId = userId;
    }

    UserData() {

    }

    private List<String> messages;
    private List<RootCategory> categories;
    private Long userId;
    private String userFirstName;
    private String userLastName;
}
