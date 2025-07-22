package com.example.EcoPOS_V2S.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tw.com.mypay.common.ActionDetails;
import tw.com.mypay.common.Constant;

import com.example.EcoPOS_V2S.R;
import com.example.EcoPOS_V2S.models.TransactionRecord;
import com.example.EcoPOS_V2S.models.Product;
import com.example.EcoPOS_V2S.models.EcoPosRequest;
import com.example.EcoPOS_V2S.config.EnvironmentConfig;

public class TransactionDetailActivity extends AppCompatActivity {

    private static final String TAG = "TransactionDetailActivity";
    //private static final String PACKAGE_NAME = "tw.com.mypay.tap.dev"; // 測試
    private static final String PACKAGE_NAME = "tw.com.mypay.tap"; //正式
    private static final String TARGET_ACTIVITY_NAME = "tw.com.mypay.MainActivity";
    private static final int REFUND_REQUEST_CODE = 1;
    private static final String CANCEL_INVOICE_URL = EnvironmentConfig.getCancelInvoiceUrl();
    private static final String APP_KEY = EnvironmentConfig.getAppKey();
    private String SELLER_TAX_ID = EnvironmentConfig.getSellerTaxId();

    private TransactionRecord record;
    private String key;
    private String uid;
    private String cancelReason;
    private String cancellerName;

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
        TextView tvCancelledBy = findViewById(R.id.tvCancelledBy);
        TextView tvCancelReason = findViewById(R.id.tvCancelReason);
        TableRow rowCancelledBy = findViewById(R.id.rowCancelledBy);
        TableRow rowCancelReason = findViewById(R.id.rowCancelReason);
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

        // 如果是已作廢狀態就顯示作廢人員與原因
        if ("已作廢".equalsIgnoreCase(record.getInvoiceStatus())) {
            rowCancelledBy.setVisibility(View.VISIBLE);
            rowCancelReason.setVisibility(View.VISIBLE);

            tvCancelledBy.setText(record.getInvalidatedBy() != null ? record.getInvalidatedBy() : "無資料");
            tvCancelReason.setText(record.getInvalidatedReason() != null ? record.getInvalidatedReason() : "無資料");
        } else {
            rowCancelledBy.setVisibility(View.GONE);
            rowCancelReason.setVisibility(View.GONE);
        }


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
                showCancelDialog();
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

    /*
    private void handleRefund() {
        String paymentType = record.getPaymentType();
        String invoiceNumber = record.getInvoiceNumber();

        // 如果沒有發票號碼，直接作廢訂單
        if (invoiceNumber == null || invoiceNumber.isEmpty() || invoiceNumber.equalsIgnoreCase("N/A") || invoiceNumber.equalsIgnoreCase("null-N/A")) {
            invalidateInvoice(String.valueOf(record.getOrderId()));
            Toast.makeText(TransactionDetailActivity.this, "訂單作廢成功", Toast.LENGTH_LONG).show();
        } else {
            // 根據付款方式進行處理
            if ("現金".equalsIgnoreCase(paymentType)) {
                // (1) 現金 => 不用外部refund，直接呼叫 Amego f0501
                new CancelInvoiceTask(invoiceNumber, new Runnable() {
                    @Override
                    public void run() {
                        // onSuccess => 再本地作廢
                        invalidateInvoice(String.valueOf(record.getOrderId()));
                    }
                }).execute();
            } else {
                // (2) 其他付款方式 => 先呼叫外部退款
                // 等待 onActivityResult 確認成功後，再 f0501 + invalidate
                if ("信用卡".equalsIgnoreCase(paymentType)) {
                    TapRefund();
                } else {
                    refund();
                }
            }
        }
    }

     */

    private void TapRefund() {
        try {
            EcoPosRequest appRequest = new EcoPosRequest();

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
            EcoPosRequest appRequest = new EcoPosRequest();

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

    private void invalidateInvoice(String orderId, String canceller, String reason)  {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");

        JSONObject invalidateData = new JSONObject();
        try {
            invalidateData.put("reason", reason);
            invalidateData.put("invalidated_by", canceller); // 新增欄位作廢人員

            RequestBody body = RequestBody.create(invalidateData.toString(), JSON);
            Log.d(TAG, "invalidateInvoice: " + invalidateData.toString());
            Request request = new Request.Builder()
                    .url(EnvironmentConfig.getOrderInvalidationUrl(orderId))
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

    private String getAppRequestJsonString(EcoPosRequest appRequest) {
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
                // 退款成功 => 先作廢「遠端」發票
                String invoiceNumber = record.getInvoiceNumber();
                if (invoiceNumber != null && !invoiceNumber.isEmpty()
                        && !invoiceNumber.equalsIgnoreCase("N/A")) {
                    new CancelInvoiceTask(invoiceNumber, new Runnable() {
                        @Override
                        public void run() {
                            // 遠端作廢成功後 => 作廢本地訂單
                            invalidateInvoice(String.valueOf(record.getOrderId()), cancellerName, cancelReason);
                        }
                    }).execute();
                } else {
                    // 沒發票 => 本地作廢
                    invalidateInvoice(String.valueOf(record.getOrderId()), cancellerName, cancelReason);
                }
            } else {
                // 退款失敗
                Toast.makeText(this, "Refund failed", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class CancelInvoiceTask extends AsyncTask<Void, Void, String> {

        private String invoiceNumber;         // 要作廢的發票號碼
        private String msg;                   // API回應訊息
        private int code = -1;
        private Runnable onSuccess;// API回應狀態碼

        public CancelInvoiceTask(String invoiceNumber, Runnable onSuccess) {
            this.invoiceNumber = invoiceNumber;
            this.onSuccess = onSuccess;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // [1] 組出要傳的 JSON Array: [ { "CancelInvoiceNumber": "XXX" } ]
                JSONArray array = new JSONArray();
                JSONObject cancelObj = new JSONObject();
                cancelObj.put("CancelInvoiceNumber", invoiceNumber);
                array.put(cancelObj);

                // [2] 依照你的 InvoiceTask 風格 =>
                //     invoice=SELLER_TAX_ID & data=URLencode(array) & time=... & sign=MD5(...)
                long currentTime = System.currentTimeMillis() / 1000;
                String arrayStr = array.toString();
                String sHashText = arrayStr + currentTime + APP_KEY;  // 與開立發票同樣簽名方式
                String signature = md5(sHashText);

                String postData = "invoice=" + SELLER_TAX_ID
                        + "&data=" + URLEncoder.encode(arrayStr, "UTF-8")
                        + "&time=" + currentTime
                        + "&sign=" + signature;

                // 呼叫 performPostCall
                String response = performPostCall(CANCEL_INVOICE_URL, postData);
                if (response == null) {
                    return "Error: 遠端作廢API無回應";
                }

                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1);
                String msg = jsonResponse.optString("msg", "No msg");

                if (code == 0) {
                    return "OK";
                } else {
                    return "Error: " + msg;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if ("OK".equals(result)) {
                // 作廢成功
                Toast.makeText(TransactionDetailActivity.this, "遠端發票作廢成功", Toast.LENGTH_LONG).show();

                // 成功後執行 onSuccess
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                // 失敗
                Toast.makeText(TransactionDetailActivity.this, "遠端發票作廢失敗: " + result,
                        Toast.LENGTH_LONG).show();
                Log.e("CancelInvoiceTask", "Error: " + result);
            }
        }

        // ===========================
        // performPostCall 同InvoiceTask
        // ===========================
        private String performPostCall(String requestURL, String postData) {
            HttpURLConnection urlConnection = null;
            BufferedWriter writer = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(requestURL);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.connect();

                OutputStream outputStream = urlConnection.getOutputStream();
                writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                writer.write(postData);
                writer.flush();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                } else {
                    // 直接讀取錯誤內容
                    BufferedReader errorReader =
                            new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    StringBuilder errorBuilder = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBuilder.append(line);
                    }
                    return "Error: HTTP " + responseCode + ", body=" + errorBuilder.toString();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (writer != null) {
                    try { writer.close(); } catch (IOException ignored) {}
                }
                if (reader != null) {
                    try { reader.close(); } catch (IOException ignored) {}
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        // ===========================
        // 計算 MD5 與 InvoiceTask 相同
        // ===========================
        private String md5(String s) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(s.getBytes());
                byte[] messageDigest = digest.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : messageDigest) {
                    String hex = Integer.toHexString(0xFF & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private void showCancelDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_refund_reason, null);
        EditText editCanceller = dialogView.findViewById(R.id.editCanceller);
        EditText editCustomReason = dialogView.findViewById(R.id.editCustomReason);
        Spinner spinnerReasons = dialogView.findViewById(R.id.spinnerReasons);

        String[] reasons = {
                "資料輸入有誤", "無法適應車輛", "臨時改變行程",
                "改變心意(更改訂單)", "忘記使用優惠", "其它：備註欄位自行填寫"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, reasons);
        spinnerReasons.setAdapter(adapter);

        TextView labelCustomReason = dialogView.findViewById(R.id.labelCustomReason);

        spinnerReasons.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == reasons.length - 1) { // "其它：備註欄位自行填寫"
                    labelCustomReason.setVisibility(View.VISIBLE);
                    editCustomReason.setVisibility(View.VISIBLE);
                } else {
                    labelCustomReason.setVisibility(View.GONE);
                    editCustomReason.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        new AlertDialog.Builder(this)
                .setTitle("請填寫作廢資訊")
                .setView(dialogView)
                .setPositiveButton("確定", (dialog, which) -> {
                    String canceller = editCanceller.getText().toString().trim();
                    String reason = spinnerReasons.getSelectedItem().toString();
                    if (reason.equals("其它：備註欄位自行填寫")) {
                        reason = editCustomReason.getText().toString().trim();
                    }

                    if (canceller.isEmpty() || reason.isEmpty()) {
                        Toast.makeText(this, "作廢人員與原因不得為空", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 儲存資訊後呼叫 handleRefund，並傳入 reason & canceller
                    handleRefundWithInfo(canceller, reason);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void handleRefundWithInfo(String canceller, String reason) {
        String paymentType = record.getPaymentType();
        String invoiceNumber = record.getInvoiceNumber();
        this.cancelReason = reason;
        this.cancellerName = canceller;

        if (invoiceNumber == null || invoiceNumber.isEmpty() || invoiceNumber.equalsIgnoreCase("N/A") || invoiceNumber.equalsIgnoreCase("null-N/A")) {
            invalidateInvoice(String.valueOf(record.getOrderId()), canceller, reason);
        } else {
            if ("現金".equalsIgnoreCase(paymentType)) {
                new CancelInvoiceTask(invoiceNumber, () ->
                        invalidateInvoice(String.valueOf(record.getOrderId()), canceller, reason)
                ).execute();
            } else {
                if ("信用卡".equalsIgnoreCase(paymentType)) {
                    TapRefund(); // 內部走 onActivityResult 時補上紀錄用資料
                } else {
                    refund(); // 同上
                }
            }
        }
    }



}
