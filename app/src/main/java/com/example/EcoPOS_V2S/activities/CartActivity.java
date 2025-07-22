package com.example.EcoPOS_V2S.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.EcoPOS_V2S.R;
import com.example.EcoPOS_V2S.adapters.CartAdapter;
import com.example.EcoPOS_V2S.models.CartItem;
import com.example.EcoPOS_V2S.config.NetworkManager;
import com.example.EcoPOS_V2S.config.DataManager;

public class CartActivity extends AppCompatActivity {
    private NetworkManager networkManager;
    private DataManager dataManager;
    private RecyclerView recyclerViewCart;
    private Button buttonProceedToPayment;
    private CartAdapter cartAdapter;
    private ArrayList<CartItem> cartItems;
    private double totalAmount;
    private double maxPercentageDiscount = 0;
    private double maxFixedDiscount = 0;

    private String getToken() {
        return dataManager.getToken();
    }

    private void fetchDiscountLimits() {
        networkManager.fetchDiscountLimits(new NetworkManager.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONObject dataObject = jsonObject.getJSONObject("data");
                    maxPercentageDiscount = dataObject.optDouble("max_percentage_discount", 0) * 100;
                    maxFixedDiscount = dataObject.optDouble("max_fixed_discount", 0);
                    
                    // Save discount limits
                    dataManager.saveDiscountLimits(maxPercentageDiscount, maxFixedDiscount);
                    
                    runOnUiThread(() -> {
                        cartAdapter.setMaxPercentageDiscount(maxPercentageDiscount);
                        cartAdapter.setMaxFixedDiscount(maxFixedDiscount);
                    });
                } catch (JSONException e) {
                    Log.e("DiscountData", "Error parsing discount data: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(String error) {
                Log.e("DiscountData", "Error fetching discount data: " + error);
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        
        // Initialize managers
        networkManager = NetworkManager.getInstance(this);
        dataManager = DataManager.getInstance(this);
        
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        
        // Load cart items and discount limits
        cartItems = (ArrayList<CartItem>) dataManager.loadCartItems();
        maxPercentageDiscount = dataManager.getMaxPercentageDiscount();
        maxFixedDiscount = dataManager.getMaxFixedDiscount();
        
        cartAdapter = new CartAdapter(this, cartItems, maxPercentageDiscount, maxFixedDiscount);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCart.setAdapter(cartAdapter);
        
        fetchDiscountLimits();

        Button buttonDiscount = findViewById(R.id.buttonDiscount);
        Button buttonRebate = findViewById(R.id.buttonRebate);

        buttonDiscount.setOnClickListener(v -> promptForDiscount());
        buttonRebate.setOnClickListener(v -> promptForRebate());

        buttonProceedToPayment = findViewById(R.id.buttonProceedToPayment);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < cartItems.size()) {
                    cartItems.remove(position);
                    cartAdapter.notifyItemRemoved(position);
                    dataManager.saveCartItems(cartItems);
                    Log.d("CartActivity", "Item removed and cart updated");
                }
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerViewCart);

        calculateTotalAmount();

        buttonProceedToPayment.setOnClickListener(v -> {
            calculateTotalAmount();
            dataManager.saveCartItems(cartItems);
            
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            intent.putExtra("totalAmount", totalAmount);
            intent.putExtra("discountAmount", cartAdapter.getDiscountAmount());
            intent.putExtra("rebateAmount", cartAdapter.getRebateAmount());
            intent.putExtra("cartItems", cartItems);
            startActivity(intent);
            Log.d("CartActivity", "Proceed to payment with total amount: " + totalAmount);
        });

        Log.d("CartActivity", "onCreate: CartActivity initialized");
    }


    private void promptForDiscount() {
        Log.d("MainActivity", "promptForDiscount called");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this)
                .setTitle("請輸入折扣金額（百分比）")
                .setView(input)
                .setPositiveButton("Apply", (dialog, whichButton) -> {
                    try {
                        double enteredValue = Double.parseDouble(input.getText().toString());
                        Log.d("MainActivity", "Entered discount percentage: " + enteredValue);
                        cartAdapter.applyDiscount(enteredValue);
                    } catch (Exception e) {
                        Toast.makeText(this, "折扣应用失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("MainActivity", "Error applying discount", e);
                    }
                })
                .setNegativeButton("Cancel", (dialog, whichButton) -> dialog.dismiss())
                .show();
    }

    private void promptForRebate() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        new AlertDialog.Builder(this)
                .setTitle("請輸入折讓金額")
                .setView(input)
                .setPositiveButton("Apply", (dialog, whichButton) -> {
                    try {
                        double enteredValue = Double.parseDouble(input.getText().toString());
                        cartAdapter.applyRebate(enteredValue);
                    } catch (Exception e) {
                        Toast.makeText(this, "折讓应用失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("MainActivity", "Error applying rebate", e);
                    }
                })
                .setNegativeButton("Cancel", (dialog, whichButton) -> dialog.dismiss())
                .show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataManager.saveCartItems(cartItems);
    }


    private void calculateTotalAmount() {
        totalAmount = 0;
        for (CartItem item : cartItems) {
            totalAmount += item.getTotalPrice();
        }
        totalAmount = Math.ceil(totalAmount); // 无条件进位
        Log.d("calculateTotalAmount", "Total Amount: " + totalAmount);
    }
}
