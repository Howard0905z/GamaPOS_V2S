package com.example.EcoPOS_V2S.models;

import java.io.Serializable;

public class Addon implements Serializable {
    private static final long serialVersionUID = 1L; // 添加这个字段
    private int addonId;
    private String name;
    private double price;
    private int addonCategoryId;
    private String addonCategoryName;

    public Addon(int addonId, String name, double price, int addonCategoryId, String addonCategoryName) {
        this.addonId = addonId;
        this.name = name;
        this.price = price;
        this.addonCategoryId = addonCategoryId;
        this.addonCategoryName = addonCategoryName;
    }

    public int getAddonId() {
        return addonId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getAddonCategoryId() {
        return addonCategoryId;
    }

    public String getAddonCategoryName() {
        return addonCategoryName;
    }
}
