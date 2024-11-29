package com.example.GamaPOS_V2S;

import java.util.List;

public class GamaPosRequest {
    private double amount;
    private String orderId;
    private String payModeId;

    private String printMode;
    private String userId = "2";
    //private String storeUid = "910022190001"; //商務代號
    private String storeUid = "662943700001"; //商務代號
    private String currency = "TWD";
    private List<Item> itemList;  // 添加 itemList
    private Invoice invoice;
    private String  creditCardReceiptType = "1";

    private String code;

    private String uid;

    private String key;

    private String discount;

    private int invoiceStateVoid;

    private boolean isPrintOrderDetailsReceipt = true;

    // Getter and Setter methods
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStoreUid() {
        return storeUid;
    }

    public String getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setPayModeId(String payModeId) {
        this.payModeId = payModeId;
    }

    public void setPrintMode(String printMode) {
        this.printMode = printMode;
    }

    public String getPayModeId() {
        return payModeId;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getCreditCardReceiptType(){return creditCardReceiptType;}

    public boolean getPrintOrderDetailsReceipt(){return isPrintOrderDetailsReceipt;}

    public boolean setPrintOrderDetailsReceipt(boolean isPrintOrderDetailsReceipt){this.isPrintOrderDetailsReceipt = isPrintOrderDetailsReceipt;
        return isPrintOrderDetailsReceipt;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getUid(){return uid;}

    public void setKey(String key) {
        this.key = key;
    }
    public String getKey(){return key;}

    public void setInvoiceState(int invoiceStateVoid) {
        this.invoiceStateVoid = invoiceStateVoid;
    }

    public void setCreditCardReceiptType(String creditCardReceiptType) {this.creditCardReceiptType = creditCardReceiptType;
    }

    public int getInvoiceState(){
        return invoiceStateVoid;
    }

    public void setDiscount(String discount){this.discount = discount;}

    public String getDiscount(){return discount;}

}

class Item {
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
