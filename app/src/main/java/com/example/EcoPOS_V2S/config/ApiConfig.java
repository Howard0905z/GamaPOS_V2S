package com.example.EcoPOS_V2S.config;

/**
 * @deprecated 請使用 EnvironmentConfig 類別進行統一配置管理
 * 這個類別保留是為了向後兼容，建議逐步遷移到 EnvironmentConfig
 */
@Deprecated
public class ApiConfig {
    // ⚠️ 已遷移到 EnvironmentConfig - 請使用 EnvironmentConfig.getBaseUrl()
    public static final String BASE_URL = com.example.EcoPOS_V2S.config.EnvironmentConfig.getBaseUrl();

    public static String getOrderInvalidationUrl(String orderId) {
        return com.example.EcoPOS_V2S.config.EnvironmentConfig.getOrderInvalidationUrl(orderId);
    }

    // 你也可以加其他 endpoint 工具方法，例如：
    // public static String getOrderDetailUrl(String orderId) { return BASE_URL + "/orders/" + orderId; }
}
