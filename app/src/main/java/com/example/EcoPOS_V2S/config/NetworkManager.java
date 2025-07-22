package com.example.EcoPOS_V2S.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private static NetworkManager instance;
    private final OkHttpClient client;
    private final Context context;
    
    private NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public static synchronized NetworkManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkManager(context);
        }
        return instance;
    }
    
    public interface ApiCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }
    
    private String getToken() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("jwt_token", "");
    }
    
    public void fetchCategories(ApiCallback callback) {
        String token = getToken();
        Request request = new Request.Builder()
                .url(com.example.EcoPOS_V2S.config.EnvironmentConfig.getCategoriesUrl())
                .addHeader("Authorization", "Bearer " + token)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void fetchAllProducts(ApiCallback callback) {
        String token = getToken();
        Request request = new Request.Builder()
                .url(com.example.EcoPOS_V2S.config.EnvironmentConfig.getProductsUrl())
                .addHeader("Authorization", "Bearer " + token)
                .build();
        
        executeRequest(request, callback);
    }
    
    public void fetchProductBySKU(String sku, ApiCallback callback) {
        String token = getToken();
        
        Request request = new Request.Builder()
                .url(com.example.EcoPOS_V2S.config.EnvironmentConfig.getProductBySKUUrl(sku))
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("accept", "application/json")
                .build();
        
        executeRequest(request, callback);
    }
    
    public void fetchDiscountLimits(ApiCallback callback) {
        String token = getToken();
        Request request = new Request.Builder()
                .url(com.example.EcoPOS_V2S.config.EnvironmentConfig.getDiscountLimitsUrl())
                .addHeader("Authorization", "Bearer " + token)
                .build();
        
        executeRequest(request, callback);
    }
    
    private void executeRequest(Request request, ApiCallback callback) {
        Log.d(TAG, "Executing request: " + request.url());
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
                callback.onFailure("網路請求失敗: " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response body: " + responseBody);
                
                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onFailure("API錯誤: " + responseBody);
                }
            }
        });
    }
}