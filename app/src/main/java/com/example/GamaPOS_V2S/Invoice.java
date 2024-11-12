package com.example.GamaPOS_V2S;

public class Invoice {
    private int rateType;
    private int inputType;
    private int cloudType;
    private String mobileCode;
    private String taxId;
    private String naturalPerson;
    private String mPostZone;
    private String mAddress;
    private String loveCode;
    private String b2bTitle;
    private String b2bId;
    private String b2bPostZone;
    private String b2bAddress;
    private String sellerId;
    private String buyerIdId;
    private int issuerState;
    private String printMode;

    // Getter and Setter methods for each field
    public int getRateType() {
        return rateType;
    }

    public void setRateType(int rateType) {
        this.rateType = rateType;
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public int getCloudType() {
        return cloudType;
    }

    public void setCloudType(int cloudType) {
        this.cloudType = cloudType;
    }

    public String getMobileCode() {
        return mobileCode;
    }

    public void setMobileCode(String mobileCode) {
        this.mobileCode = mobileCode;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getNaturalPerson() {
        return naturalPerson;
    }

    public void setNaturalPerson(String naturalPerson) {
        this.naturalPerson = naturalPerson;
    }

    public String getmPostZone() {
        return mPostZone;
    }

    public void setmPostZone(String mPostZone) {
        this.mPostZone = mPostZone;
    }

    public String getmAddress() {
        return mAddress;
    }

    public void setmAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public String getLoveCode() {
        return loveCode;
    }

    public void setLoveCode(String loveCode) {
        this.loveCode = loveCode;
    }

    public String getB2bTitle() {
        return b2bTitle;
    }

    public void setB2bTitle(String b2bTitle) {
        this.b2bTitle = b2bTitle;
    }

    public String getB2bId() {
        return b2bId;
    }

    public void setB2bId(String b2bId) {
        this.b2bId = b2bId;
    }

    public String getB2bPostZone() {
        return b2bPostZone;
    }

    public void setB2bPostZone(String b2bPostZone) {
        this.b2bPostZone = b2bPostZone;
    }

    public String getB2bAddress() {
        return b2bAddress;
    }

    public void setB2bAddress(String b2bAddress) {
        this.b2bAddress = b2bAddress;
    }

    public int getIssuerState() {
        return issuerState;
    }

    public void setIssuerState(int issuerState) {
        this.issuerState = issuerState;
    }

    public String getPrintMode() {
        return printMode;
    }

    public void setPrintMode(String printMode) {
        this.printMode = printMode;
    }
    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getBuyerIdId() {
        return buyerIdId;
    }

    public void setBuyerIdId(String buyerIdId) {
        this.buyerIdId = buyerIdId;
    }
}