package com.example.GamaPOS_V2S;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
    //private static final String PACKAGE_NAME = "tw.com.mypay.tap"; //正式

    private static final String PACKAGE_NAME = "tw.com.mypay.tap.dev"; //測試
    private static final String TARGET_ACTIVITY_NAME = "tw.com.mypay.MainActivity";
    private static final int REFUND_REQUEST_CODE = 1;

    private TransactionRecord record;
    private String key;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        LinearLayout detailLayout = findViewById(R.id.detailLayout);
        Button buttonRefund = findViewById(R.id.buttonRefund);

        // 接收傳遞過來的資料
        record = (TransactionRecord) getIntent().getSerializableExtra("transactionRecord");
        key = getIntent().getStringExtra("key");
        uid = getIntent().getStringExtra("uid");

        // 打印 key 和 uid 以檢查其值
        Log.d(TAG, "Received key: " + key);
        Log.d(TAG, "Received uid: " + uid);

        // 顯示訂單細節
        addDetailRow(detailLayout, "訂單時間", record.getTransactionDate());
        addDetailRow(detailLayout, "訂單編號", String.valueOf(record.getOrderId()));
        addDetailRow(detailLayout, "發票號碼", record.getInvoiceNumber());
        addDetailRow(detailLayout, "統一編號", record.getTaxIdNumber());
        addDetailRow(detailLayout, "結帳金額", String.valueOf(record.getInvoiceAmount()));
        addDetailRow(detailLayout, "付款方式", record.getPaymentType());

        // 顯示商品名稱列表
        for (Product product : record.getProducts()) {
            addDetailRow(detailLayout, "商品名稱", product.getProductName() + " x" + product.getQuantity());
        }

        // 設置退款按鈕的點擊事件
        buttonRefund.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String invoiceNumber = record.getInvoiceNumber();
                //Toast.makeText(TransactionDetailActivity.this, "No invoice number. Invoice invalidated." + record.getInvoiceNumber(), Toast.LENGTH_LONG).show();
                if (invoiceNumber == null || invoiceNumber.isEmpty() || invoiceNumber.equalsIgnoreCase("N/A") || invoiceNumber.equalsIgnoreCase("null-N/A")) {
                    // 如果沒有發票號碼，直接調用 invalidateInvoice
                    invalidateInvoice(String.valueOf(record.getOrderId()));
                    Toast.makeText(TransactionDetailActivity.this, "訂單作廢成功", Toast.LENGTH_LONG).show();
                } else {
                    // 有發票號碼，進行退款流程
                    refund();
                }
            }
        });
    }

    private void addDetailRow(LinearLayout layout, String label, String value) {
        TextView textView = new TextView(this);
        textView.setText(String.format("%s: %s", label, value));
        textView.setPadding(0, 16, 0, 16);
        layout.addView(textView);
    }

    private void refund() {
        try {
            GamaPosRequest appRequest = new GamaPosRequest();

            // 設定作廢所需的必要參數
            appRequest.getStoreUid(); // 需要設置特約商店商務代號
            appRequest.setOrderId("A2024111916390002");
            appRequest.setAmount(record.getInvoiceAmount());
            appRequest.getUserId(); // 需要設置使用者ID

            // 設定作廢發票的必要參數
            appRequest.setKey(key);
            appRequest.setUid(uid);
            appRequest.setInvoiceState(Constant.INVOICE_STATE_VOID);  // 作廢或作廢重開
            appRequest.setCreditCardReceiptType("1");

            // 再次打印 key 和 uid 以檢查其值
            Log.d(TAG, "Refund key: " + key);
            Log.d(TAG, "Refund uid: " + uid);

            // 將 appRequest 轉為 JSON 並記錄在 Log 中
            String requestJson = getAppRequestJsonString(appRequest);
            Log.d(TAG, "Refund Request JSON: " + requestJson);

            ActionDetails actionDetails = new ActionDetails();
            actionDetails.setAction(Constant.ACTION_TAP_REFUND);
            actionDetails.setData(requestJson);  // 取得 app request 的 JSON 字串

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            this.startActivityForResult(intent, REFUND_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start refund activity", e);
            Toast.makeText(this, "Failed to start refund activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 新增 invalidateInvoice 方法，接受 String 類型的 orderId
    private void invalidateInvoice(String orderId) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");

        JSONObject invalidateData = new JSONObject();
        try {
            invalidateData.put("reason", "作廢");  // 設置原因

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
                    } else {
                        Log.e(TAG, "Invoice invalidation failed with code: " + response.code());
                        Log.e(TAG, "Response body: " + response.body().string());
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
            jsonObject.put("invoiceState", appRequest.getInvoiceState());
            jsonObject.put("creditCardReceiptType", appRequest.getCreditCardReceiptType());
            // Add other necessary fields if needed
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    // 在 onActivityResult 方法中添加調用 invalidateInvoice 的邏輯
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REFUND_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // 退款成功後調用 invalidateInvoice
                invalidateInvoice(String.valueOf(record.getOrderId()));
            } else {
                Toast.makeText(this, "Refund failed", Toast.LENGTH_LONG).show();
            }
        }
    }
}
