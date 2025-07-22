package com.example.EcoPOS_V2S.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.example.EcoPOS_V2S.models.CartItem;

public class DataManager {
    private static final String TAG = "DataManager";
    private static DataManager instance;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    private DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences("appPrefs", Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }
    
    public void saveCartItems(List<CartItem> cartItems) {
        String cartItemsJson = gson.toJson(cartItems);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("cartItems", cartItemsJson);
        editor.apply();
        Log.d(TAG, "Cart items saved: " + cartItemsJson);
    }
    
    public List<CartItem> loadCartItems() {
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");
        Type listType = new TypeToken<ArrayList<CartItem>>() {}.getType();
        List<CartItem> cartItems = gson.fromJson(cartItemsJson, listType);
        
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }
        
        Log.d(TAG, "Cart items loaded: " + cartItemsJson);
        return cartItems;
    }
    
    public void clearCartItems() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("cartItems", "[]");
        editor.apply();
        Log.d(TAG, "Cart items cleared");
    }
    
    public void saveToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("jwt_token", token);
        editor.putBoolean("is_logged_in", true);
        editor.apply();
        Log.d(TAG, "Token saved");
    }
    
    public String getToken() {
        return sharedPreferences.getString("jwt_token", "");
    }
    
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean("is_logged_in", false);
    }
    
    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("jwt_token");
        editor.putBoolean("is_logged_in", false);
        editor.apply();
        Log.d(TAG, "User logged out");
    }
    
    public void saveDiscountLimits(double maxPercentageDiscount, double maxFixedDiscount) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("max_percentage_discount", (float) maxPercentageDiscount);
        editor.putFloat("max_fixed_discount", (float) maxFixedDiscount);
        editor.apply();
        Log.d(TAG, "Discount limits saved");
    }
    
    public double getMaxPercentageDiscount() {
        return sharedPreferences.getFloat("max_percentage_discount", 0f);
    }
    
    public double getMaxFixedDiscount() {
        return sharedPreferences.getFloat("max_fixed_discount", 0f);
    }
}