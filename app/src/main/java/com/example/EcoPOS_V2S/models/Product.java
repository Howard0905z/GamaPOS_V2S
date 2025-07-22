package com.example.EcoPOS_V2S.models;

import java.io.Serializable;
import java.util.List;

public class Product implements Serializable {
    private int productId;
    private String productName;
    private Integer price;
    private Integer quantity;
    private List<Addon> addons;

    public Product() {}

    public Product(int productId, String productName, Integer price, Integer quantity, List<Addon> addons) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.addons = addons;
    }

    public Product(int productId, String productName, int price, List<Addon> addons) {
        this(productId, productName, price, null, addons);
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) { // 將參數類型改為 Integer
        this.quantity = quantity;
    }

    public List<Addon> getAddons() {
        return addons;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setPrice(Integer price) { // 修改參數類型並確保賦值正確
        this.price = price;
    }
}
