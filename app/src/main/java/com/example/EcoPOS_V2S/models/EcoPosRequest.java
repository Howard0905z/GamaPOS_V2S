package com.example.EcoPOS_V2S.models;

import java.util.List;

import com.example.EcoPOS_V2S.config.EnvironmentConfig;

public class EcoPosRequest {
    private double amount;
    private String orderId;
    private String payModeId;

    private String printMode;
    private String userId = com.example.EcoPOS_V2S.config.EnvironmentConfig.getUserId();
    private String storeUid = com.example.EcoPOS_V2S.config.EnvironmentConfig.getStoreUid();
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
