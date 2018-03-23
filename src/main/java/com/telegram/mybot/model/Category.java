package com.telegram.mybot.model;

public class Category extends BaseCategory {


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String value;

    public Category( String name ) {
        super( name );
    }

    public Category( String name, String value ) {
        super( name );
        this.value = value;
    }

    Category() {

    }
}
