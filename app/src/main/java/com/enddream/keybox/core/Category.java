package com.enddream.keybox.core;

public class Category {
    public String id = "";
    public String name = "";
    public long order;

    public Category() {}

    public Category(String id, String name, long order) {
        this.id = id;
        this.name = name;
        this.order = order;
    }
}
