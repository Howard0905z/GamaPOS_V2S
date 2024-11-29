package com.example.GamaPOS_V2S;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tw.com.mypay.common.ActionDetails;
import tw.com.mypay.common.Constant;

public class TransactionDetailActivity extends AppCompatActivity {

    private static final String TAG = "TransactionDetailActivity";
    //private static final String PACKAGE_NAME = "tw.com.mypay.tap.dev"; // 測試
    private static final String PACKAGE_NAME = "tw.com.mypay.tap"; //正式
    private static final String TARGET_ACTIVITY_NAME = "tw.com.mypay.MainActivity";
    private static final int REFUND_REQUEST_CODE = 1;

    private TransactionRecord record;
    private String key;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        // 初始化視圖
        TextView orderTime = findViewById(R.id.orderTime);
        TextView orderNumber = findViewById(R.id.orderNumber);
        TextView orderStatus = findViewById(R.id.orderStatus);
        TextView orderInvoice = findViewById(R.id.orderInvoice);
        TextView orderTaxId = findViewById(R.id.orderTaxId);
        TextView orderAmount = findViewById(R.id.orderAmount);
        TextView orderPayment = findViewById(R.id.orderPayment);
        TableLayout productTable = findViewById(R.id.productTable);
        Button buttonInvalidate = findViewById(R.id.buttonInvalidate);

        // 接收傳遞過來的資料
        record = (TransactionRecord) getIntent().getSerializableExtra("transactionRecord");
        key = getIntent().getStringExtra("key");
        uid = getIntent().getStringExtra("uid");

        // 日誌檢查
        Log.d(TAG, "Received key: " + key);
        Log.d(TAG, "Received uid: " + uid);

        // 填充訂單資料
        orderTime.setText(record.getTransactionDate());
        orderNumber.setText(String.valueOf(record.getOrderNumber()));
        orderStatus.setText(record.getInvoiceStatus());
        setStatusColor(orderStatus, record.getInvoiceStatus());
        orderInvoice.setText(record.getInvoiceNumber());
        orderTaxId.setText(record.getTaxIdNumber());
        orderAmount.setText(String.format("$%.2f", record.getInvoiceAmount()));
        orderPayment.setText(record.getPaymentType());

        // 填充商品列表
        for (Product product : record.getProducts()) {
            TableRow row = new TableRow(this);

            // 商品名稱
            TextView productName = new TextView(this);
            productName.setText(product.getProductName());
            productName.setPadding(18, 8, 8, 8);
            productName.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)); // 權重設置較大以分配更多空間

            // 商品數量
            TextView productQuantity = new TextView(this);
            productQuantity.setText(String.format("x%s", product.getQuantity()));
            productQuantity.setPadding(8, 8, 8, 8);
            productQuantity.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); // 權重設置較小以分配較少空間
            productQuantity.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END); // 文字靠右對齊

            // 添加到行
            row.addView(productName);
            row.addView(productQuantity);

            // 添加行到表格
            productTable.addView(row);
        }


        // 設置作廢按鈕的點擊事件
        buttonInvalidate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRefund();
            }
        });
    }

    private void setStatusColor(TextView textView, String status) {
        int color = getStatusColor(status);
        textView.setBackgroundColor(color);
    }

    private int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "已上傳":
                return Color.parseColor("#A5D6A7"); // 綠色
            case "處理中":
                return Color.parseColor("#FFF176"); // 黃色
            case "已作廢":
                return Color.parseColor("#EF9A9A"); // 紅色
            default:
                return Color.parseColor("#E0E0E0"); // 灰色
        }
    }

    private void handleRefund() {
        String paymentType = record.getPaymentType();
        String invoiceNumber = record.getInvoiceNumber();

        // 如果沒有發票號碼，直接作廢訂單
        if (invoiceNumber == null || invoiceNumber.isEmpty() || invoiceNumber.equalsIgnoreCase("N/A") || invoiceNumber.equalsIgnoreCase("null-N/A")) {
            invalidateInvoice(String.valueOf(record.getOrderId()));
            Toast.makeText(TransactionDetailActivity.this, "訂單作廢成功", Toast.LENGTH_LONG).show();
        } else {
            // 根據付款方式進行處理
            if ("信用卡".equalsIgnoreCase(paymentType)) {
                TapRefund(); // 信用卡退款
            } else {
                refund(); // 其他付款方式退款
            }
        }
    }

    private void TapRefund() {
        try {
            GamaPosRequest appRequest = new GamaPosRequest();

            appRequest.getStoreUid(); // 設置特約商店商務代號
            appRequest.setOrderId(record.getOrderNumber());
            appRequest.setAmount(record.getInvoiceAmount());
            appRequest.getUserId(); // 設置使用者ID
            appRequest.setKey(key);
            appRequest.setUid(uid);

            String requestJson = getAppRequestJsonString(appRequest);
            Log.d(TAG, "Refund Request JSON: " + requestJson);

            ActionDetails actionDetails = new ActionDetails();
            actionDetails.setAction(Constant.ACTION_TAP_REFUND);
            actionDetails.setData(requestJson);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            this.startActivityForResult(intent, REFUND_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start TapRefund activity", e);
            Toast.makeText(this, "Failed to start TapRefund activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refund() {
        try {
            GamaPosRequest appRequest = new GamaPosRequest();

            appRequest.getStoreUid(); // 設置特約商店商務代號
            appRequest.setOrderId(record.getOrderNumber());
            appRequest.setAmount(record.getInvoiceAmount());
            appRequest.getUserId(); // 設置使用者ID
            appRequest.setKey(key);
            appRequest.setUid(uid);

            String requestJson = getAppRequestJsonString(appRequest);
            Log.d(TAG, "Refund Request JSON: " + requestJson);

            ActionDetails actionDetails = new ActionDetails();
            actionDetails.setAction(Constant.ACTION_REFUND);
            actionDetails.setData(requestJson);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            this.startActivityForResult(intent, REFUND_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start refund activity", e);
            Toast.makeText(this, "Failed to start refund activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void invalidateInvoice(String orderId) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");

        JSONObject invalidateData = new JSONObject();
        try {
            invalidateData.put("reason", "作廢");

            RequestBody body = RequestBody.create(invalidateData.toString(), JSON);
            Request request = new Request.Builder()
                    .url("http://172.207.27.24:8100/orders/" + orderId + "/invalidated_invoice")
                    .patch(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Invoice invalidation failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Invoice invalidated successfully");
                        runOnUiThread(() -> {
                            Toast.makeText(TransactionDetailActivity.this, "訂單作廢成功", Toast.LENGTH_LONG).show();

                            // 回到 TransactionRecordsActivity
                            Intent intent = new Intent(TransactionDetailActivity.this, TransactionRecordsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish(); // 關閉當前頁面
                        });
                    } else {
                        Log.e(TAG, "Invoice invalidation failed with code: " + response.code());
                        String responseBody = response.body().string();
                        Log.e(TAG, "Response body: " + responseBody);
                        runOnUiThread(() -> Toast.makeText(TransactionDetailActivity.this, "作廢失敗: " + responseBody, Toast.LENGTH_LONG).show());
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getAppRequestJsonString(GamaPosRequest appRequest) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("storeUid", appRequest.getStoreUid());
            jsonObject.put("orderId", appRequest.getOrderId());
            jsonObject.put("amount", appRequest.getAmount());
            jsonObject.put("userId", appRequest.getUserId());
            jsonObject.put("key", appRequest.getKey());
            jsonObject.put("uid", appRequest.getUid());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REFUND_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                invalidateInvoice(String.valueOf(record.getOrderId()));
            } else {
                Toast.makeText(this, "Refund failed", Toast.LENGTH_LONG).show();
            }
        }
    }
}
