package com.example.GamaPOS_V2S;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TransactionRecordsActivity extends AppCompatActivity implements Serializable {

    private Button buttonQueryOrders;
    private Button buttonQueryRevenue;
    private TextView textViewRevenueInfo;
    private TextView textViewSelectedDate;
    private RecyclerView recyclerViewTransactions;
    private TransactionsAdapter transactionsAdapter;
    private List<TransactionRecord> transactionList = new ArrayList<>();
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_records);

        buttonQueryOrders = findViewById(R.id.buttonQueryOrders);
        buttonQueryRevenue = findViewById(R.id.buttonQueryRevenue);
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate);
        textViewRevenueInfo = findViewById(R.id.textViewRevenueInfo);
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions);

        transactionsAdapter = new TransactionsAdapter(transactionList, this::showRecordDetails);
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTransactions.setAdapter(transactionsAdapter);

        buttonQueryOrders.setOnClickListener(v -> showDatePickerDialog("orders"));
        buttonQueryRevenue.setOnClickListener(v -> showDatePickerDialog("revenue"));
    }

    private void showDatePickerDialog(String queryType) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // 格式化日期為 yyyy-MM-dd
                    selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    textViewSelectedDate.setText(selectedDate);
                    if (queryType.equals("orders")) {
                        fetchTransactions(selectedDate);
                    } else if (queryType.equals("revenue")) {
                        fetchRevenue(selectedDate);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }


    private void fetchTransactions(String date) {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");
        OkHttpClient client = new OkHttpClient();
        String url = "http://172.207.27.24:8100/orders?start_date=" + date;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(TransactionRecordsActivity.this, "Failed to fetch transactions", Toast.LENGTH_SHORT).show());
                Log.e("TransactionRecords", "Failed to fetch transactions", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray dataArray = jsonResponse.getJSONArray("data");

                        transactionList.clear();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject transactionJson = dataArray.getJSONObject(i);

                            List<Product> products = new ArrayList<>();
                            JSONArray productsArray = transactionJson.getJSONArray("products");
                            for (int j = 0; j < productsArray.length(); j++) {
                                JSONObject productJson = productsArray.getJSONObject(j);
                                Product product = new Product();
                                product.setProductId(productJson.getInt("product_id"));
                                product.setProductName(productJson.getString("product_name"));
                                product.setPrice((int) productJson.getDouble("unit_price"));
                                product.setQuantity(productJson.getInt("quantity"));
                                products.add(product);
                            }

                            // 提取 key 和 uid
                            String key = transactionJson.optString("key");
                            String uid = transactionJson.optString("uid");

                            TransactionRecord transaction = new TransactionRecord(
                                    transactionJson.optString("order_id"),
                                    transactionJson.optString("invoice_status", "N/A"),
                                    transactionJson.optString("payment_type", "N/A"),
                                    transactionJson.optString("invoice_number", "N/A"),
                                    transactionJson.optDouble("invoice_amount", 0),
                                    transactionJson.optDouble("total_amount_excluding_tax", 0),
                                    transactionJson.optDouble("tax_amount", 0),
                                    transactionJson.optString("tax_id_number", "N/A"),
                                    transactionJson.optString("transaction_date", "N/A"),
                                    transactionJson.optString("creator", "N/A"),
                                    transactionJson.optString("invalidated_user", "N/A"),
                                    products,
                                    key,
                                    uid
                            );
                            transactionList.add(transaction);
                        }

                        runOnUiThread(() -> transactionsAdapter.notifyDataSetChanged());

                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(TransactionRecordsActivity.this, "Failed to parse transactions", Toast.LENGTH_SHORT).show());
                        Log.e("TransactionRecords", "Failed to parse transactions", e);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(TransactionRecordsActivity.this, "Failed to fetch transactions", Toast.LENGTH_SHORT).show());
                    Log.e("TransactionRecords", "Failed to fetch transactions with code: " + response.code());
                }
            }
        });
    }

    private void fetchRevenue(String date) {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");
        OkHttpClient client = new OkHttpClient();
        String url = "http://172.207.27.24:8100/reports/revenues/today?date=" + date;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(TransactionRecordsActivity.this, "Failed to fetch revenue", Toast.LENGTH_SHORT).show());
                Log.e("TransactionRecords", "Failed to fetch revenue", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        Log.d("TransactionRecords", "Revenue response: " + responseData);
                        JSONObject jsonResponse = new JSONObject(responseData);

                        // 檢查 dataObject 是否存在
                        if (!jsonResponse.has("data")) {
                            Log.e("TransactionRecords", "No 'data' field in response");
                            runOnUiThread(() -> Toast.makeText(TransactionRecordsActivity.this, "No data available", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        JSONObject dataObject = jsonResponse.getJSONObject("data");

                        // 取得總營收
                        double totalRevenue = dataObject.optDouble("total_revenue", 0);

                        // 取得各支付方式的營收
                        JSONArray paymentRevenues = dataObject.optJSONArray("payment_revenues");
                        StringBuilder paymentRevenuesBuilder = new StringBuilder();
                        paymentRevenuesBuilder.append("當日總營收: ").append(totalRevenue).append("\n\n");
                        paymentRevenuesBuilder.append("各支付方式營收:\n");

                        if (paymentRevenues != null) {
                            for (int i = 0; i < paymentRevenues.length(); i++) {
                                JSONObject paymentRevenue = paymentRevenues.getJSONObject(i);
                                String paymentMethod = paymentRevenue.optString("payment_method", "Unknown");
                                double paymentAmount = paymentRevenue.optDouble("payment_revenue", 0);
                                paymentRevenuesBuilder.append(paymentMethod)
                                        .append(": ")
                                        .append(paymentAmount)
                                        .append("\n");
                            }
                        } else {
                            paymentRevenuesBuilder.append("No payment revenues available.\n");
                        }

                        String finalPaymentRevenues = paymentRevenuesBuilder.toString();
                        runOnUiThread(() -> {
                            // 顯示當日總營收和各支付方式的營收
                            textViewRevenueInfo.setVisibility(View.VISIBLE); // 設為可見
                            textViewRevenueInfo.setText(finalPaymentRevenues);
                        });
                    } catch (JSONException e) {
                        runOnUiThread(() -> Toast.makeText(TransactionRecordsActivity.this, "Failed to parse revenue", Toast.LENGTH_SHORT).show());
                        Log.e("TransactionRecords", "Failed to parse revenue", e);
                    }
                } else {
                    String errorResponse = response.body().string();
                    Log.e("TransactionRecords", "Failed to fetch revenue: " + errorResponse);
                    runOnUiThread(() -> Toast.makeText(TransactionRecordsActivity.this, "Failed to fetch revenue", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showRecordDetails(TransactionRecord record) {
        // 确保 key 和 uid 在这里被正确设置
        Log.d(TAG, "Passing key: " + record.getKey() + ", uid: " + record.getUid());
        Intent intent = new Intent(this, TransactionDetailActivity.class);
        intent.putExtra("transactionRecord", record);
        intent.putExtra("key", record.getKey());
        intent.putExtra("uid", record.getUid());
        startActivity(intent);
    }
}
