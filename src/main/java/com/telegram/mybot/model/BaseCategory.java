package com.telegram.mybot.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

public class BaseCategory implements Serializable {
    private String name;
    private Long id;

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    BaseCategory(String name) {
        this.name = name;
    }

    BaseCategory() {

    }
}
