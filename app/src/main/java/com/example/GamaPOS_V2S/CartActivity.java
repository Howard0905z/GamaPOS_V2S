package com.example.GamaPOS_V2S;

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

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerViewCart;
    private Button buttonProceedToPayment;
    private CartAdapter cartAdapter;
    private ArrayList<CartItem> cartItems;
    private double totalAmount;
    private double maxPercentageDiscount = 0;
    private double maxFixedDiscount = 0;

    private String getToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("jwt_token", "");
    }

    private void fetchDiscountLimits() {
        OkHttpClient client = new OkHttpClient();
        String token = getToken();
        Request request = new Request.Builder()
                .url("http://172.207.27.24:8100/user/disconuts")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error fetching discount data: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                int statusCode = response.code();
                final String responseBody = response.body().string();
                Log.d("DiscountData", "HTTP Status Code: " + statusCode);
                Log.d("DiscountData", "Response Body: " + responseBody);

                if (statusCode >= 200 && statusCode < 300) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONObject dataObject = jsonObject.getJSONObject("data");
                        maxPercentageDiscount = dataObject.optDouble("max_percentage_discount", 0) * 100; // 轉換為百分比
                        maxFixedDiscount = dataObject.optDouble("max_fixed_discount", 0);

                        // 在主執行緒中更新 UI
                        runOnUiThread(() -> {
                            // 確保 `CartAdapter` 使用最新的折扣限制
                            cartAdapter.setMaxPercentageDiscount(maxPercentageDiscount);
                            cartAdapter.setMaxFixedDiscount(maxFixedDiscount);
                        });
                    } catch (JSONException e) {
                        Log.e("DiscountData", "Error parsing discount data: " + e.getMessage());
                        //runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error parsing discount data", Toast.LENGTH_LONG).show());
                    }
                } else {
                    //runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch discount data", Toast.LENGTH_LONG).show());
                }
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        recyclerViewCart = findViewById(R.id.recyclerViewCart);

        // 初始化 cartItems
        loadCartItemsFromPreferences();

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
                    cartItems.remove(position); // 从列表中删除
                    cartAdapter.notifyItemRemoved(position); // 通知适配器更新
                    saveCartItemsToPreferences(); // 保存更新后的购物车到 SharedPreferences
                    Log.d("CartActivity", "Item removed and cart updated");
                }
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerViewCart);

        calculateTotalAmount();

        buttonProceedToPayment.setOnClickListener(v -> {
            calculateTotalAmount(); // 确保总金额是最新的
            saveCartItemsToPreferences(); // 保存更新后的购物车数据到 SharedPreferences

            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            intent.putExtra("totalAmount", totalAmount);
            intent.putExtra("discountAmount", cartAdapter.getDiscountAmount());
            intent.putExtra("rebateAmount", cartAdapter.getRebateAmount());
            intent.putExtra("cartItems", cartItems); // 使用 putSerializableExtra 传递 ArrayList<CartItem>
            startActivity(intent);
            Log.d("CartActivity", "Proceed to payment with total amount: " + totalAmount);
        });

        Log.d("CartActivity", "onCreate: CartActivity initialized");
    }

    private void loadCartItemsFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");
        cartItems = new Gson().fromJson(cartItemsJson, new TypeToken<ArrayList<CartItem>>() {}.getType());
        Log.d("CartActivity", "Loaded cart items from preferences: " + cartItemsJson);
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
        saveCartItemsToPreferences(); // 保存到 SharedPreferences
    }

    private void saveCartItemsToPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String cartItemsJson = new Gson().toJson(cartItems); // 序列化最新的购物车列表
        editor.putString("cartItems", cartItemsJson);
        editor.apply(); // 应用更改
        Log.d("CartActivity", "Cart items saved to preferences");
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
