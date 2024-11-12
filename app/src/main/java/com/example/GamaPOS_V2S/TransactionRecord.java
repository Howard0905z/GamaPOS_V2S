package com.example.GamaPOS_V2S;

import java.io.Serializable;
import java.util.List;

public class TransactionRecord implements Serializable {
    private String orderId;
    private String invoiceStatus;
    private String paymentType;
    private String invoiceNumber;
    private double invoiceAmount;
    private double totalAmountExcludingTax;
    private double taxAmount;
    private String taxIdNumber;
    private String transactionDate;
    private String creator;
    private String invalidatedUser;
    private List<Product> products;
    private String key; // 添加 key 字段
    private String uid; // 添加 uid 字段

    public TransactionRecord(String orderId, String invoiceStatus, String paymentType, String invoiceNumber, double invoiceAmount,
                             double totalAmountExcludingTax, double taxAmount, String taxIdNumber, String transactionDate,
                             String creator, String invalidatedUser, List<Product> products, String key, String uid) {
        this.orderId = orderId;
        this.invoiceStatus = invoiceStatus;
        this.paymentType = paymentType;
        this.invoiceNumber = invoiceNumber;
        this.invoiceAmount = invoiceAmount;
        this.totalAmountExcludingTax = totalAmountExcludingTax;
        this.taxAmount = taxAmount;
        this.taxIdNumber = taxIdNumber;
        this.transactionDate = transactionDate;
        this.creator = creator;
        this.invalidatedUser = invalidatedUser;
        this.products = products;
        this.key = key; // 初始化 key 字段
        this.uid = uid; // 初始化 uid 字段
    }

    public String getOrderId() {
        return orderId;
    }

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public double getInvoiceAmount() {
        return invoiceAmount;
    }

    public double getTotalAmountExcludingTax() {
        return totalAmountExcludingTax;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public String getTaxIdNumber() {
        return taxIdNumber;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public String getCreator() {
        return creator;
    }

    public String getInvalidatedUser() {
        return invalidatedUser;
    }

    public List<Product> getProducts() {
        return products;
    }

    public String getKey() {
        return key; // 添加 getter 方法
    }

    public String getUid() {
        return uid; // 添加 getter 方法
    }
}
