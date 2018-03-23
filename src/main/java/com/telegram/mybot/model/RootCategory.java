package com.telegram.mybot.model;

import java.util.ArrayList;
import java.util.List;

public class RootCategory extends BaseCategory {

    public List<Category> getCategories() {
        if (categories == null) {
            categories = new ArrayList<>();
        }
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    private List<Category> categories;

    public RootCategory ( String name ) {
        super( name );
    }

    public RootCategory ( String name, List<Category> categories ) {
        super( name );
        this.categories = categories;
    }

    RootCategory() {

    }
}
