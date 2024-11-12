package com.example.GamaPOS_V2S;

public class GamaPosResponse {
    private String status;
    private String message;
    private double invoiceAmount;
    private String invoiceNumber;

    private String wordTrack;
    private String taxIdNumber;

    private String result;

    private String msg;

    private String code;

    private String uid;

    private String key;

    // Getter and Setter methods
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMsg(){return msg;}

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode(){return code;}

    public void setCode(String code) {
        this.code = code;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public Double getInvoiceAmount() {
        return invoiceAmount;
    }

    public void setInvoiceAmount(Double invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getWordTrack(){return wordTrack;}

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setWordTrack(String wordTrack){this.wordTrack = wordTrack;}

    public String getTaxIdNumber() {
        return taxIdNumber;
    }

    public void setTaxIdNumber(String taxIdNumber) {
        this.taxIdNumber = taxIdNumber;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getUid(){return uid;}

    public void setKey(String key) {
        this.key = key;
    }
    public String getKey(){return key;}


}
