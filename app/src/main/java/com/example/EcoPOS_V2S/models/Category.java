package com.example.EcoPOS_V2S.models;

import java.util.List;

public class Category {
    private int category_id;
    private String name;
    private List<SubCategory> sub_categories;
    private List<Product> products; // 新增的字段
    private boolean isSelected;

    // Getters and Setters
    public int getCategory_id() {
        return category_id;
    }

    public void setCategory_id(int category_id) {
        this.category_id = category_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SubCategory> getSub_categories() {
        return sub_categories;
    }

    public void setSub_categories(List<SubCategory> sub_categories) {
        this.sub_categories = sub_categories;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
