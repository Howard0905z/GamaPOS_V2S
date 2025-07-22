package com.example.EcoPOS_V2S.models;

public class Item {
    private int id;
    private String name;
    private String price; // 折扣后的价格
    private String quantity;
    private String total;
    private boolean isPrintOut;

    // 添加原价字段
    private String originalPrice;

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public boolean isPrintOut() {
        return isPrintOut;
    }

    public void setPrintOut(boolean isPrintOut) {
        this.isPrintOut = isPrintOut;
    }

    // 原价的 getter 和 setter 方法
    public String getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }
}