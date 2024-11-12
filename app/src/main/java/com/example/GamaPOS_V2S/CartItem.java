package com.example.GamaPOS_V2S;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L; // 添加这个字段
    private int id;
    private String name;
    private double price;
    private double originalPrice; // 原价
    private int quantity;
    private double totalPrice;
    private double discountedPrice;
    private boolean isSelected;
    private boolean isPrintOut;
    private List<Addon> addons;

    public CartItem(int id, String name, double price, int quantity, boolean isPrintOut) {
        this.id = id;
        this.name = name;
        this.originalPrice = price; // 将价格初始值赋予原价
        this.price = price;
        this.quantity = quantity;
        this.totalPrice = price * quantity;
        this.discountedPrice = price; // 默认情况下，折扣价格等于初始价格
        this.isPrintOut = isPrintOut;
        this.isSelected = false;
        this.addons = new ArrayList<>();
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPrice = this.price * quantity; // 更新总价
    }


    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isPrintOut() {
        return isPrintOut;
    }

    public double getDiscountedTotalPrice() {
        return discountedPrice;
    }

    public List<Addon> getAddons() {
        return addons;
    }

    public void setAddons(List<Addon> addons) {
        this.addons = addons;
    }
}
