package com.example.EcoPOS_V2S.config;

public class EnvironmentConfig {
    
    // 🔧 環境切換 - 只需要修改這個值
    private static final Environment CURRENT_ENVIRONMENT = Environment.PRODUCTION;
    
    // 環境枚舉
    public enum Environment {
        DEVELOPMENT, // 開發環境
        TESTING,     // 測試環境
        PRODUCTION   // 生產環境
    }
    
    // ==================== API 配置 ====================
    
    // API 基礎地址
    public static String getBaseUrl() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "http://172.207.27.24:8100";
            case TESTING:
                return "http://172.207.27.24:8100";
            case PRODUCTION:
                return "http://172.207.27.24:8100";
            default:
                return "http://172.207.27.24:8100";
        }
    }
    
    // 發票API地址
    public static String getInvoiceUrl() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "https://invoice-api.amego.tw/json/f0401";
            case TESTING:
                return "https://invoice-api.amego.tw/json/f0401";
            case PRODUCTION:
                return "https://invoice-api.amego.tw/json/f0401";
            default:
                return "https://invoice-api.amego.tw/json/f0401";
        }
    }
    
    // 取消發票API地址
    public static String getCancelInvoiceUrl() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "https://invoice-api.amego.tw/json/f0501";
            case TESTING:
                return "https://invoice-api.amego.tw/json/f0501";
            case PRODUCTION:
                return "https://invoice-api.amego.tw/json/f0501";
            default:
                return "https://invoice-api.amego.tw/json/f0501";
        }
    }
    
    // ==================== 金流配置 ====================
    
    // MyPay APP KEY
    public static String getAppKey() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "sHeq7t8G1wiQvhAuIM27"; // 測試環境的 APP KEY
            case TESTING:
                return "sHeq7t8G1wiQvhAuIM27"; // 測試環境的 APP KEY
            case PRODUCTION:
                return "qlodw7veyDcO6UqhpKV2"; // 生產環境的 APP KEY
            default:
                return "qlodw7veyDcO6UqhpKV2";
        }
    }
    
    // MyPay 套件名稱
    public static String getPackageName() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "tw.com.mypay.tap.dev"; // 測試環境套件名稱
            case TESTING:
                return "tw.com.mypay.tap.dev"; // 測試環境套件名稱
            case PRODUCTION:
                return "tw.com.mypay.tap"; // 生產環境套件名稱
            default:
                return "tw.com.mypay.tap";
        }
    }
    
    // ==================== 商家配置 ====================
    
    // 商家統一編號
    public static String getSellerTaxId() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "12345678"; // 測試統一編號
            case TESTING:
                return "12345678"; // 測試統一編號
            case PRODUCTION:
                return "00365362"; // 生產統一編號
            default:
                return "00365362";
        }
    }
    
    // 商家代號
    public static String getStoreUid() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "910022190001"; // 測試商務代號
            case TESTING:
                return "910022190001"; // 測試商務代號
            case PRODUCTION:
                return "003653620001"; // 生產商務代號 (請修改為實際值)
            default:
                return "003653620001";
        }
    }
    
    // 預設用戶ID
    public static String getUserId() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "1"; // 測試用戶ID
            case TESTING:
                return "1"; // 測試用戶ID
            case PRODUCTION:
                return "2"; // 生產用戶ID
            default:
                return "2";
        }
    }
    
    // ==================== 其他配置 ====================
    
    // 是否啟用調試模式
    public static boolean isDebugMode() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return true;
            case TESTING:
                return true;
            case PRODUCTION:
                return false;
            default:
                return false;
        }
    }
    
    // 獲取當前環境名稱
    public static String getEnvironmentName() {
        switch (CURRENT_ENVIRONMENT) {
            case DEVELOPMENT:
                return "開發環境";
            case TESTING:
                return "測試環境";
            case PRODUCTION:
                return "生產環境";
            default:
                return "未知環境";
        }
    }
    
    // ==================== API 端點工具方法 ====================
    
    public static String getLoginUrl() {
        return getBaseUrl() + "/login";
    }
    
    public static String getCategoriesUrl() {
        return getBaseUrl() + "/categories";
    }
    
    public static String getProductsUrl() {
        return getBaseUrl() + "/v2/products";
    }
    
    public static String getProductBySKUUrl(String sku) {
        return getBaseUrl() + "/products/SKU/multiple?SKU=" + sku;
    }
    
    public static String getPaymentMethodsUrl() {
        return getBaseUrl() + "/user/payment_methods";
    }
    
    public static String getDiscountLimitsUrl() {
        return getBaseUrl() + "/user/disconuts";
    }
    
    public static String getOrdersUrl() {
        return getBaseUrl() + "/orders";
    }
    
    public static String getOrderNumberUrl() {
        return getBaseUrl() + "/orders/number";
    }
    
    public static String getOrderInvalidationUrl(String orderId) {
        return getBaseUrl() + "/orders/" + orderId + "/invalidated_invoice";
    }
    
    public static String getRevenueReportUrl(String date) {
        return getBaseUrl() + "/reports/revenues/today?date=" + date;
    }
    
    public static String getOrdersWithDateUrl(String date) {
        return getBaseUrl() + "/orders?start_date=" + date;
    }
}