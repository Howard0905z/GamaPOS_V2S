package com.example.EcoPOS_V2S.activities;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;


import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import com.caverock.androidsvg.SVG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import com.example.EcoPOS_V2S.models.Item;
import com.example.EcoPOS_V2S.models.CartItem;
import com.example.EcoPOS_V2S.models.Invoice;
import com.example.EcoPOS_V2S.models.EcoPosRequest;
import com.example.EcoPOS_V2S.models.EcoPosResponse;
import com.example.EcoPOS_V2S.adapters.CartAdapter;
import com.example.EcoPOS_V2S.config.EnvironmentConfig;

public class CheckoutActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String PACKAGE_NAME = EnvironmentConfig.getPackageName();
    private static final String TARGET_ACTIVITY_NAME = "tw.com.mypay.MainActivity";
    private static final String INVOICE_URL = EnvironmentConfig.getInvoiceUrl();
    private static final String APP_KEY = EnvironmentConfig.getAppKey();

    private static final int LINE_PAY_REQUEST_CODE = 1;
    private static final int CREDIT_CARD_REQUEST_CODE = 2;
    private static final int CASH_REQUEST_CODE = 3;
    private static final int SCAN_CODE = 4;
    private static final int LINE_PAY_OFFLINE_CODE = 5;
    private static final int STORE_CREDIT_CARD_CODE = 6;
    private static final int ECO_PAY_CODE = 7;
    private static final int CULTURE_COIN_CODE = 8;
    private static final int PX_PAY_CODE = 8;
    private static final int EASY_PAY_CODE = 8;
    private static final int JKO_PAY_CODE = 8;

    private double discountAmount;
    private double rebateAmount;


    private EditText editTextTotal, editTextNumber, editEmployeeNumber, editInvoiceRemarks, editDealerCode;
    private TextView textViewChange;
    private EditText currentEditText;
    private LinearLayout buttonContainer;
    private CheckBox checkBoxPrintReceipt;

    private SunmiPrinterService sunmiPrinterService = null;

    private static int orderCounter = 0;

    private String currentOrderId;
    private static String lastOrderTime = "";

    private String totalString;
    private String loveCode = "";
    private String uniformNumber = "0000000000";

    private String carrierCode = "";
    private String SELLER_TAX_ID = EnvironmentConfig.getSellerTaxId();
    private String invoiceNumber = ""; // 儲存發票號碼供列印使用

    private List<Item> itemList;
    private ArrayList<CartItem> cartItems;
    private Invoice invoice;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "OrderNumberPrefs";
    private static final String KEY_ORDER_NUMBER = "OrderNumber";

    private CartAdapter cartAdapter; // 声明 CartAdapter
    private String fetchedOrderNumber = null;

    private InnerPrinterCallback innerPrinterCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            sunmiPrinterService = service;
            // 可以在这里调用打印方法了
        }

        @Override
        protected void onDisconnected() {
            sunmiPrinterService = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
        double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);

        Log.d("CheckoutActivity", "Discount Amount: " + discountAmount);
        Log.d("CheckoutActivity", "Rebate Amount: " + rebateAmount);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!sharedPreferences.contains(KEY_ORDER_NUMBER)) {
            sharedPreferences.edit().putInt(KEY_ORDER_NUMBER, 1).apply();
        }

        editTextTotal = findViewById(R.id.editTextTotal);
        editTextNumber = findViewById(R.id.editTextNumber);
        textViewChange = findViewById(R.id.textViewChange);
        currentEditText = editTextNumber;

        editEmployeeNumber = findViewById(R.id.editEmployeeNumber);
        editEmployeeNumber.setText("1"); // 預設顯示1

        editInvoiceRemarks = findViewById(R.id.editInvoiceRemarks);
        editDealerCode = findViewById(R.id.editDealerCode);

        checkBoxPrintReceipt = findViewById(R.id.checkBoxPrintReceipt);

        Button buttonCash = findViewById(R.id.buttonCash);
        Button buttonLinePay = findViewById(R.id.buttonLinePay);
        Button buttonCreditCard = findViewById(R.id.buttonCreditCard);
        Button buttonStoreCreditCard = findViewById(R.id.buttonStoreCreditCard);
        Button buttonEcoPay = findViewById(R.id.buttonEcoPay);
        Button buttonCultureCoin = findViewById(R.id.buttonCultureCoin);
        Button buttonCreditCardReceived = findViewById(R.id.buttonCreditCardReceived);
        Button buttonCashReceived = findViewById(R.id.buttonCashReceived);
        Button buttonLinePayReceived = findViewById(R.id.buttonLinePayReceived);
        Button buttonShoppingMallGiftCardReceived = findViewById(R.id.buttonShoppingMallGiftCardReceived);
        Button buttonCultureCoinReceived = findViewById(R.id.buttonCultureCoinReceived);
        Button buttonPxPay = findViewById(R.id.buttonPxPay);
        Button buttonEasyPay = findViewById(R.id.buttonEasyPay);
        Button buttonJkoPay = findViewById(R.id.buttonJkoPay);

        editEmployeeNumber.setOnTouchListener((v, event) -> {
            currentEditText = editEmployeeNumber;
            setFocusOn(editEmployeeNumber);
            return false; // <-- 讓原生行為繼續執行
        });

        editTextNumber.setOnTouchListener((v, event) -> {
            currentEditText = editTextNumber;
            setFocusOn(editTextNumber);
            return false;
        });

        try {
            boolean result = InnerPrinterManager.getInstance().bindService(this, innerPrinterCallback);
            if (!result) {
                Toast.makeText(this, "绑定打印服务失败", Toast.LENGTH_SHORT).show();
            }
        } catch (InnerPrinterException e) {
            e.printStackTrace();
            Toast.makeText(this, "绑定打印服务时发生异常", Toast.LENGTH_SHORT).show();
        }


        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");
        OkHttpClient client = new OkHttpClient();

        // 建立 Request
        Request request = new Request.Builder()
                .url(EnvironmentConfig.getPaymentMethodsUrl())
                .addHeader("Authorization", "Bearer " + token)
                .build();

        // 發送請求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CheckoutActivity", "API Request Failed", e);
                e.printStackTrace();
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    Log.d("CheckoutActivity", "API Response: " + responseData);

                    // 解析回傳的 JSON 並處理例外
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONObject dataObject = jsonObject.getJSONObject("data");
                        Log.d("CheckoutActivity", "API Response: " + responseData);

                        // 獲取每個支付方法的狀態
                        final boolean cashEnabled = dataObject.optBoolean("CASH", false);
                        final boolean linePayEnabled = dataObject.optBoolean("LINEPAY", false);
                        final boolean creditCardEnabled = dataObject.optBoolean("CREDIT_CARD", false);
                        final boolean ecoPayEnabled = dataObject.optBoolean("ECO_PAY", false);
                        final boolean storeCreditCardEnabled = dataObject.optBoolean("SHOPPING_MALL_CREDIT_CARD", false);
                        final boolean cultureCoinEnabled = dataObject.optBoolean("CULTURE_COIN", false);

                        final boolean cashReceivedEnabled = dataObject.optBoolean("CASH_RECEIVED", false);
                        final boolean linePayReceivedEnabled = dataObject.optBoolean("LINEPAY_RECEIVED", false);
                        final boolean creditCardReceivedEnabled = dataObject.optBoolean("CREDIT_CARD_RECEIVED", false);
                        final boolean shoppingMallGiftCardReceivedEnabled = dataObject.optBoolean("SHOPPING_MALL_GIFT_CARD_RECEIVED", false);
                        final boolean cultureCoinReceivedEnabled = dataObject.optBoolean("CULTURE_COIN_RECEIVED", false);

                        // 在主執行緒中更新 UI
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    buttonCash.setVisibility(cashEnabled ? View.VISIBLE : View.GONE);
                                    buttonLinePay.setVisibility(linePayEnabled ? View.VISIBLE : View.GONE);
                                    buttonCreditCard.setVisibility(creditCardEnabled ? View.VISIBLE : View.GONE);
                                    buttonEcoPay.setVisibility(ecoPayEnabled ? View.VISIBLE : View.GONE);
                                    buttonStoreCreditCard.setVisibility(storeCreditCardEnabled ? View.VISIBLE : View.GONE);
                                    buttonCultureCoin.setVisibility(cultureCoinEnabled ? View.VISIBLE : View.GONE);

                                    buttonCashReceived.setVisibility(cashReceivedEnabled ? View.VISIBLE : View.GONE);
                                    buttonLinePayReceived.setVisibility(linePayReceivedEnabled ? View.VISIBLE : View.GONE);
                                    buttonCreditCardReceived.setVisibility(creditCardReceivedEnabled ? View.VISIBLE : View.GONE);
                                    buttonShoppingMallGiftCardReceived.setVisibility(shoppingMallGiftCardReceivedEnabled ? View.VISIBLE : View.GONE);
                                    buttonCultureCoinReceived.setVisibility(cultureCoinReceivedEnabled ? View.VISIBLE : View.GONE);

                                    buttonContainer.removeAllViews();
                                    if (cashEnabled) buttonContainer.addView(buttonCash);
                                    if (linePayEnabled) buttonContainer.addView(buttonLinePay);
                                    if (creditCardEnabled) buttonContainer.addView(buttonCreditCard);
                                    if (ecoPayEnabled) buttonContainer.addView(buttonEcoPay);
                                    if (storeCreditCardEnabled) buttonContainer.addView(buttonStoreCreditCard);
                                    if (cultureCoinEnabled) buttonContainer.addView(buttonCultureCoin);

                                    if (cashReceivedEnabled) buttonContainer.addView(buttonCashReceived);
                                    if (linePayReceivedEnabled) buttonContainer.addView(buttonLinePayReceived);
                                    if (creditCardReceivedEnabled) buttonContainer.addView(buttonCreditCardReceived);
                                    if (shoppingMallGiftCardReceivedEnabled) buttonContainer.addView(buttonShoppingMallGiftCardReceived);
                                    if (cultureCoinReceivedEnabled) buttonContainer.addView(buttonCultureCoinReceived);
                                } catch (Exception e) {
                                    Log.e("CheckoutActivity", "Error setting button visibility", e);
                                }
                            }
                        });

                    } catch (Exception e) {
                        Log.e("CheckoutActivity", "Error parsing JSON or setting button visibility", e);
                    }
                }else {
                    Log.e("CheckoutActivity", "Unsuccessful API Response: " + response.code());
                }
            }
        });

        Intent intent = getIntent();
        double totalAmountFromCart = intent.getDoubleExtra("totalAmount", 0);
        Log.d("CheckoutActivity", "Total Amount from Cart: " + totalAmountFromCart);
        editTextTotal.setText(String.valueOf(totalAmountFromCart));

        cartItems = (ArrayList<CartItem>) intent.getSerializableExtra("cartItems");

        if (cartItems != null) {
            for (CartItem cartItem : cartItems) {
                Log.d("CheckoutActivity", "Cart Item: " + cartItem.getName() + ", Discounted Price: " + cartItem.getDiscountedPrice());
            }
        } else {
            Log.d("CheckoutActivity", "No cart items received, initializing empty cart");
        }

        itemList = new ArrayList<>();
        if (cartItems != null) {
            for (CartItem cartItem : cartItems) {
                Item item = new Item();
                item.setId(cartItem.getId());
                item.setName(cartItem.getName());
                item.setOriginalPrice(String.valueOf(cartItem.getOriginalPrice()));
                item.setPrice(String.valueOf(cartItem.getPrice())); // 折扣後的價格
                item.setQuantity(String.valueOf(cartItem.getQuantity()));
                item.setTotal(String.valueOf(cartItem.getTotalPrice()));
                item.setPrintOut(cartItem.isPrintOut());
                itemList.add(item);
            }
            Log.d("CheckoutActivity", "Received cart items: " + new Gson().toJson(cartItems));
        } else {
            cartItems = new ArrayList<>();
            Log.d("CheckoutActivity", "No cart items received, initializing empty cart");
        }

        buttonCash.setOnClickListener(v -> {
            fetchOrderNumber(new FetchOrderNumberCallback() {
                @Override
                public void onSuccess(String orderNumber) {
                    new InvoiceTask(orderNumber) {
                        @Override
                        protected void onPostExecute(String result) {
                            super.onPostExecute(result);
                            if ("OK".equals(result)) {
                                // ★★ 發票開立成功後再 createOrder
                                // 取得發票號碼
                                String invoiceNo = getInvoiceNumber();
                                // 現金交易通常沒有像 LINEPAY 那樣會回傳 key/uid，
                                // 所以可以用空字串或 null 代表，若後端不需要就帶空即可。
                                EcoPosResponse resp = new EcoPosResponse();
                                resp.setInvoiceNumber(invoiceNo);
                                resp.setUid("");
                                resp.setKey("");

                                // 呼叫 createOrder，記得 paymentType 傳 "CASH"
                                createOrder(resp, "CASH");

                                // 清空購物車、回到首頁
                                clearItemsAndReturnToHome();
                                saveCartItemsToPreferences();

                            } else {
                                // 發票開立失敗
                                Toast.makeText(CheckoutActivity.this, result, Toast.LENGTH_LONG).show();
                            }

                            // 重置載具、愛心碼、統編
                            carrierCode = "";
                            loveCode = "";
                            uniformNumber = "0000000000";
                        }
                    }.execute();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Failed to fetch order number: " + errorMessage);
                    Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });



        buttonLinePay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOrderNumber(new FetchOrderNumberCallback() {
                    @Override
                    public void onSuccess(String orderNumber) {
                        sendOfflineTransaction(); // 訂單編號成功獲取後執行結帳

                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch order number: " + errorMessage);
                        Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        buttonCreditCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOrderNumber(new FetchOrderNumberCallback() {
                    @Override
                    public void onSuccess(String orderNumber) {

                        sendCreditCardTapTransaction(); // 訂單編號成功獲取後執行結帳
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch order number: " + errorMessage);
                        Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        buttonPxPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOrderNumber(new FetchOrderNumberCallback() {
                    @Override
                    public void onSuccess(String orderNumber) {
                        sendPxPayTransaction(); // 訂單編號成功獲取後執行結帳
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch order number: " + errorMessage);
                        Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        buttonEasyPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOrderNumber(new FetchOrderNumberCallback() {
                    @Override
                    public void onSuccess(String orderNumber) {
                        sendEasyPayTransaction(); // 訂單編號成功獲取後執行結帳
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch order number: " + errorMessage);
                        Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        buttonJkoPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOrderNumber(new FetchOrderNumberCallback() {
                    @Override
                    public void onSuccess(String orderNumber) {
                        sendJkoPayTransaction(); // 訂單編號成功獲取後執行結帳
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch order number: " + errorMessage);
                        Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        setupTextWatchers();
        findViewById(R.id.buttonCE).setOnClickListener(this::onButtonClick);
        findViewById(R.id.buttonDelete).setOnClickListener(this::onButtonClick);
        findViewById(R.id.buttonOtherService).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button7).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button8).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button9).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button4).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button5).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button6).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button1).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button2).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button3).setOnClickListener(this::onButtonClick);
        findViewById(R.id.button0).setOnClickListener(this::onButtonClick);
    }

    private void collectInvoiceData() {
        if (invoice == null) {
            invoice = new Invoice();
            invoice.setRateType(1);
            invoice.setInputType(3); //實體發票：3
            invoice.setIssuerState(1);
            invoice.setPrintMode("2");
        }
    }

    private void onButtonClick(View view) {
        if (currentEditText == null) return;

        int id = view.getId();
        if (id == R.id.buttonCE) {
            currentEditText.setText("");
        } else if (id == R.id.buttonDelete) {
            String text = currentEditText.getText().toString();
            if (!text.isEmpty()) {
                currentEditText.setText(text.substring(0, text.length() - 1));
            }
        } else if (id == R.id.button7) {
            currentEditText.append("7");
        } else if (id == R.id.button8) {
            currentEditText.append("8");
        } else if (id == R.id.button9) {
            currentEditText.append("9");
        } else if (id == R.id.button4) {
            currentEditText.append("4");
        } else if (id == R.id.button5) {
            currentEditText.append("5");
        } else if (id == R.id.button6) {
            currentEditText.append("6");
        } else if (id == R.id.button1) {
            currentEditText.append("1");
        } else if (id == R.id.button2) {
            currentEditText.append("2");
        } else if (id == R.id.button3) {
            currentEditText.append("3");
        } else if (id == R.id.button0) {
            currentEditText.append("0");
        } else if (id == R.id.buttonOtherService) {
            showOtherServiceDialog();
        }
    }

    private void setFocusOn(EditText targetEditText) {
        // 重設全部背景
        editTextNumber.setBackgroundResource(R.drawable.edittext_default);
        editEmployeeNumber.setBackgroundResource(R.drawable.edittext_default);

        // 設定目前焦點欄位
        currentEditText = targetEditText;
        targetEditText.setBackgroundResource(R.drawable.edittext_focused);
    }


    private void showOtherServiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("請選擇操作");
        builder.setMessage("請選擇你要進行的操作。");
        builder.setPositiveButton("掃描載具", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //showCarrierInputDialog();
                scanCode();
            }
        });
        builder.setNeutralButton("愛心碼與統編", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showLoveCodeAndUniformNumberDialog();
            }
        });
        /*
        builder.setNegativeButton("發票作廢", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showInvoiceCancellationDialog();
            }
        });
        */
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showLoveCodeAndUniformNumberDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_love_code_and_uniform_number, null);
        EditText editTextLoveCode = view.findViewById(R.id.editTextLoveCode);
        EditText editTextUniformNumber = view.findViewById(R.id.editTextUniformNumber);

        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("請輸入愛心碼與統編");
        inputDialog.setView(view);

        inputDialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loveCode = editTextLoveCode.getText().toString().trim();
                uniformNumber = editTextUniformNumber.getText().toString().trim();

                if (!loveCode.isEmpty()) {
                    Toast.makeText(CheckoutActivity.this, "愛心碼: " + loveCode, Toast.LENGTH_LONG).show();
                }

                uniformNumber = uniformNumber.isEmpty() ? "0000000000" : uniformNumber;
            }
        });

        inputDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        inputDialog.show();
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                calculateChange();
            }
        };

        editTextTotal.addTextChangedListener(textWatcher);
        editTextNumber.addTextChangedListener(textWatcher);
    }

    private void calculateChange() {
        totalString = editTextTotal.getText().toString();
        String numberString = editTextNumber.getText().toString();

        if (!totalString.isEmpty() && !numberString.isEmpty()) {
            try {
                double total = Double.parseDouble(totalString);
                double number = Double.parseDouble(numberString);
                double change = number - total;
                textViewChange.setText(String.format("找零：%.2f", change));
            } catch (NumberFormatException e) {
                textViewChange.setText("請輸入有效的數字！");
            }
        } else {
            textViewChange.setText("請輸入總額和收款金額！");
        }
    }

    private synchronized String generateOrderId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        if (currentTime.equals(lastOrderTime)) {
            orderCounter++;
        } else {
            orderCounter = 1;
            lastOrderTime = currentTime;
        }

        return "A" + currentTime + String.format(Locale.getDefault(), "%04d", orderCounter);
    }

    private void sendOfflineTransaction() {  // 所有線下交易，掃碼收款
        try {
        double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
        String orderId = generateOrderId();

        EcoPosRequest appRequest = new EcoPosRequest();
        appRequest.setAmount(totalAmount);
        appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
        appRequest.setPayModeId(Constant.BUY_39_PAY_MODE_ID_OFFLINE_ALL);
        appRequest.setItemList(itemList);
        ActionDetails actionDetails = getTransactionActionDetails(appRequest);
        Intent intent = new Intent();
        intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
        intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);

        Log.d(TAG, "Starting LinePayActivityOffline with amount: " + totalAmount + " and orderId: " + orderId);
        this.startActivityForResult(intent, LINE_PAY_OFFLINE_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LinePayOfflineActivity", e);
            Toast.makeText(this, "Failed to start LinePayOfflineActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendCreditCardTapTransaction() {
        try {// 信用卡Tap交易
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            String orderId = generateOrderId();

            EcoPosRequest appRequest = new EcoPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId(Constant.PAY_MODE_ID_CREDIT_CARD);
            appRequest.setItemList(itemList);
            appRequest.setCreditCardReceiptType("1");
            ActionDetails actionDetails = getTapTransactionActionDetails(appRequest);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            startActivityForResult(intent, CREDIT_CARD_REQUEST_CODE);
        }catch (Exception e) {
            Log.e(TAG, "Failed to start CreditCardActivity", e);
            Toast.makeText(this, "Failed to start CreditCardActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendPxPayTransaction() {  // 所有線下交易，掃碼收款
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            String orderId = generateOrderId();

            EcoPosRequest appRequest = new EcoPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId(Constant.BUY_39_PAY_MODE_ID_OFFLINE_ALL);
            appRequest.setItemList(itemList);
            ActionDetails actionDetails = getTransactionActionDetails(appRequest);
            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);

            Log.d(TAG, "Starting LinePayActivityOffline with amount: " + totalAmount + " and orderId: " + orderId);
            this.startActivityForResult(intent, PX_PAY_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LinePayOfflineActivity", e);
            Toast.makeText(this, "Failed to start LinePayOfflineActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendEasyPayTransaction() {  // 所有線下交易，掃碼收款
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            String orderId = generateOrderId();

            EcoPosRequest appRequest = new EcoPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId(Constant.BUY_39_PAY_MODE_ID_OFFLINE_ALL);
            appRequest.setItemList(itemList);
            ActionDetails actionDetails = getTransactionActionDetails(appRequest);
            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);

            Log.d(TAG, "Starting LinePayActivityOffline with amount: " + totalAmount + " and orderId: " + orderId);
            this.startActivityForResult(intent, EASY_PAY_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LinePayOfflineActivity", e);
            Toast.makeText(this, "Failed to start LinePayOfflineActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendJkoPayTransaction() {  // 所有線下交易，掃碼收款
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            String orderId = generateOrderId();

            EcoPosRequest appRequest = new EcoPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId(Constant.BUY_39_PAY_MODE_ID_OFFLINE_ALL);
            appRequest.setItemList(itemList);
            ActionDetails actionDetails = getTransactionActionDetails(appRequest);
            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);

            Log.d(TAG, "Starting LinePayActivityOffline with amount: " + totalAmount + " and orderId: " + orderId);
            this.startActivityForResult(intent, JKO_PAY_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LinePayOfflineActivity", e);
            Toast.makeText(this, "Failed to start LinePayOfflineActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearItemsAndReturnToHome() {
        // 清空购物车数据
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("cartItems", "[]"); // 清空购物车数据
        editor.apply(); // 使用 apply 确保立即写入

        // 添加日誌確認 SharedPreferences 是否清空
        String cartItemsPref = sharedPreferences.getString("cartItems", "未找到数据");
        Log.d(TAG, "SharedPreferences 中的購物車數據 (清空後): " + cartItemsPref);

        // 清空内存中的购物车列表
        if (cartItems != null) {
            cartItems.clear();
        }
        Log.d(TAG, "内存中的購物車列表已清空");

        // 保存清空的購物車狀態
        saveCartItemsToPreferences();

        if (cartAdapter != null) {
            cartAdapter.notifyDataSetChanged(); // 通知适配器数据变化
            Log.d(TAG, "購物車適配器已通知數據變更");
        }

        // 返回主界面並設置標誌
        Intent intent = new Intent(this, com.example.EcoPOS_V2S.activities.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("fromCheckout", true); // 設置標誌
        startActivity(intent);
        finish();
    }

    private void loadCartItemsFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", null);
        if (cartItemsJson != null) {
            cartItems = new Gson().fromJson(cartItemsJson, new TypeToken<ArrayList<CartItem>>() {}.getType());
            Log.d("CartActivity", "Loaded cart items from preferences: " + cartItemsJson);
        } else {
            cartItems = new ArrayList<>(); // 確保 cartItems 不為空
            Log.d("CartActivity", "No cart items found in preferences, initializing empty cart");
        }
    }

    private ActionDetails getTransactionActionDetails(EcoPosRequest appRequest) {
        ActionDetails actionDetails = new ActionDetails();
        actionDetails.setAction(Constant.ACTION_SEND_TRANSACTION);
        actionDetails.setData(getAppRequestJsonString(appRequest));
        return actionDetails;
    }

    private ActionDetails getTapTransactionActionDetails(EcoPosRequest appRequest) {
        ActionDetails actionDetails = new ActionDetails();
        actionDetails.setAction(Constant.ACTION_TAP);
        actionDetails.setData(getAppRequestJsonString(appRequest));
        return actionDetails;
    }

    private String getAppRequestJsonString(EcoPosRequest appRequest) {
        String strDiscountAmount = String.valueOf(getIntent().getDoubleExtra("discountAmount", 0));
        String strRebateAmount = String.valueOf(getIntent().getDoubleExtra("rebateAmount", 0));

        double discount = Double.parseDouble(strDiscountAmount);
        double rebate = Double.parseDouble(strRebateAmount);

        double totalDiscount = -(discount + rebate);

        Log.d("getAppRequestJsonString", "Discount Amount: " + discount);
        Log.d("getAppRequestJsonString", "Rebate Amount: " + rebate);
        Log.d("getAppRequestJsonString", "Total Discount: " + totalDiscount);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("amount", appRequest.getAmount());
            jsonObject.put("orderId", appRequest.getOrderId());
            jsonObject.put("storeUid", appRequest.getStoreUid());
            jsonObject.put("userId", appRequest.getUserId());
            jsonObject.put("currency", appRequest.getCurrency());
            jsonObject.put("payModeId", appRequest.getPayModeId());
            jsonObject.put("creditCardReceiptType", appRequest.getCreditCardReceiptType());

            Log.d("getAppRequestJsonString", "Order Details: " + jsonObject.toString());

            JSONArray itemArray = new JSONArray();
            for (Item item : appRequest.getItemList()) {
                JSONObject itemObject = new JSONObject();
                itemObject.put("id", item.getId());
                itemObject.put("name", item.getName());
                itemObject.put("price", item.getPrice());
                itemObject.put("quantity", item.getQuantity());
                itemObject.put("total", item.getTotal());
                itemObject.put("isPrintOut", item.isPrintOut());
                itemArray.put(itemObject);

                Log.d("getAppRequestJsonString", "Item: " + itemObject.toString());
            }
            jsonObject.put("itemList", itemArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String jsonString = jsonObject.toString();
        Log.d("getAppRequestJsonString", "Final JSON: " + jsonString);
        return jsonString;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if ((requestCode == LINE_PAY_REQUEST_CODE || requestCode == CREDIT_CARD_REQUEST_CODE || requestCode == CASH_REQUEST_CODE || requestCode == SCAN_CODE || requestCode == STORE_CREDIT_CARD_CODE || requestCode == ECO_PAY_CODE || requestCode == LINE_PAY_OFFLINE_CODE || requestCode == CULTURE_COIN_CODE || requestCode == PX_PAY_CODE || requestCode == EASY_PAY_CODE || requestCode == JKO_PAY_CODE) && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "Handling result for requestCode=" + requestCode);

            ActionDetails actionDetails = data.getParcelableExtra(Constant.INTENT_EXTRA_KEY_RESPONSE_ACTION_DETAILS);
            if (actionDetails != null) {
                String responseData = actionDetails.getData();
                Log.d(TAG, "Received action details data: " + responseData);

                EcoPosResponse appResponse = getAppResponse(responseData);  // 轉換為你的 app response

                // Log the uid and key
                Log.d(TAG, "UID: " + appResponse.getUid());
                Log.d(TAG, "Key: " + appResponse.getKey());

                // Use the uid and key as needed
                if (requestCode == CREDIT_CARD_REQUEST_CODE) {
                    handleCreditCardResponse(appResponse, fetchedOrderNumber);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode == SCAN_CODE){
                    handleScanResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode == LINE_PAY_OFFLINE_CODE) {
                    handleLinePayOfflineResponse(appResponse, fetchedOrderNumber);
                    cartItems.clear(); // Clear the cart items
                } else if(requestCode == PX_PAY_CODE) {
                    handlePxPayResponse(appResponse, fetchedOrderNumber);
                    cartItems.clear(); // Clear the cart items
                } else if(requestCode == EASY_PAY_CODE) {
                    handleEasyPayResponse(appResponse, fetchedOrderNumber);
                    cartItems.clear(); // Clear the cart items
                } else if(requestCode == JKO_PAY_CODE) {
                    handleJkoPayResponse(appResponse, fetchedOrderNumber);
                    cartItems.clear(); // Clear the cart items
                } else {
                    Log.e(TAG, "Unknown request code: " + requestCode);
                }
            } else {
                Log.e(TAG, "ActionDetails is null");
            }
        } else {
            Log.e(TAG, "Data is null or request code does not match");
        }
    }

    protected void onPause() {
        super.onPause();
        saveCartItemsToPreferences(); // 保存最新的购物车数据到 SharedPreferences
    }

    private void saveCartItemsToPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String cartItemsJson = new Gson().toJson(cartItems); // 序列化最新的购物车列表
        editor.putString("cartItems", cartItemsJson);
        editor.apply(); // 应用更改
        Log.d(TAG, "Cart items saved to preferences");
    }

    private EcoPosResponse getAppResponse(String data) {
        EcoPosResponse response = new EcoPosResponse();
        try {
            JSONObject jsonObject = new JSONObject(data);
            response.setStatus(jsonObject.optString("code"));
            response.setMessage(jsonObject.optString("msg"));
            response.setCode(jsonObject.optString("code"));
            response.setMsg(jsonObject.optString("msg"));
            response.setResult(jsonObject.optString("result"));

            // Ensure to fetch the uid and key
            response.setUid(jsonObject.optString("uid"));
            response.setKey(jsonObject.optString("key"));

            Log.d(TAG, "Parsed response: " + jsonObject.toString());
            Log.d(TAG, "UID: " + response.getUid());
            Log.d(TAG, "Key: " + response.getKey());

            // 獲取發票信息
            JSONObject invoiceJson = jsonObject.optJSONObject("invoice");
            if (invoiceJson != null) {
                response.setInvoiceAmount(invoiceJson.has("amount") && !invoiceJson.isNull("amount") ? invoiceJson.getDouble("amount") : null);
                response.setInvoiceNumber(invoiceJson.optString("number"));
                response.setWordTrack(invoiceJson.optString("wordTrack"));
                response.setTaxIdNumber(invoiceJson.optString("taxId"));
                response.setB2bId(invoiceJson.optString("b2bId"));
            }

            Log.d(TAG, "Parsed response: " + jsonObject.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse response JSON", e);
        }
        return response;
    }

    private void handleLinePayOfflineResponse(EcoPosResponse response, String orderNumber) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "Line Pay Offline Status: " + status + ", Message: " + message);

            Toast.makeText(this, "Line Pay Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();

            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);



                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                new InvoiceTask(orderNumber) {
                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        // 等真正開立發票成功後再建立訂單
                        if ("OK".equals(result)) {
                            String invoiceNo = getInvoiceNumber();
                            String key = response.getKey();
                            String uid = response.getUid();
                            // 這裡拿到的 invoiceNumber 已經不再是 null
                            EcoPosResponse resp = new EcoPosResponse();
                            resp.setInvoiceNumber(invoiceNo);
                            resp.setUid(uid);  // 設定uid
                            resp.setKey(key);
                            // 可能還有其他屬性要設定

                            createOrder(resp, "LINEPAY");
                        } else {
                            // 發票開立失敗，可看你要不要繼續 createOrder(沒有發票)、或是顯示錯誤
                            Toast.makeText(CheckoutActivity.this, "發票開立失敗", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            }
        }
    }

    private void handlePxPayResponse(EcoPosResponse response, String orderNumber) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();

            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);



                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                new InvoiceTask(orderNumber) {
                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        // 等真正開立發票成功後再建立訂單
                        if ("OK".equals(result)) {
                            String invoiceNo = getInvoiceNumber();
                            String key = response.getKey();
                            String uid = response.getUid();
                            // 這裡拿到的 invoiceNumber 已經不再是 null
                            EcoPosResponse resp = new EcoPosResponse();
                            resp.setInvoiceNumber(invoiceNo);
                            resp.setUid(uid);  // 設定uid
                            resp.setKey(key);
                            // 可能還有其他屬性要設定

                            createOrder(resp, "PX_PAY");
                        } else {
                            // 發票開立失敗，可看你要不要繼續 createOrder(沒有發票)、或是顯示錯誤
                            Toast.makeText(CheckoutActivity.this, "發票開立失敗", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            }
        }
    }

    private void handleEasyPayResponse(EcoPosResponse response, String orderNumber) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();

            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);



                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                new InvoiceTask(orderNumber) {
                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        // 等真正開立發票成功後再建立訂單
                        if ("OK".equals(result)) {
                            String invoiceNo = getInvoiceNumber();
                            String key = response.getKey();
                            String uid = response.getUid();
                            // 這裡拿到的 invoiceNumber 已經不再是 null
                            EcoPosResponse resp = new EcoPosResponse();
                            resp.setInvoiceNumber(invoiceNo);
                            resp.setUid(uid);  // 設定uid
                            resp.setKey(key);
                            // 可能還有其他屬性要設定

                            createOrder(resp, "EZPAY");
                        } else {
                            // 發票開立失敗，可看你要不要繼續 createOrder(沒有發票)、或是顯示錯誤
                            Toast.makeText(CheckoutActivity.this, "發票開立失敗", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            }
        }
    }

    private void handleJkoPayResponse(EcoPosResponse response, String orderNumber) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();

            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);



                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                new InvoiceTask(orderNumber) {
                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        // 等真正開立發票成功後再建立訂單
                        if ("OK".equals(result)) {
                            String invoiceNo = getInvoiceNumber();
                            String key = response.getKey();
                            String uid = response.getUid();
                            // 這裡拿到的 invoiceNumber 已經不再是 null
                            EcoPosResponse resp = new EcoPosResponse();
                            resp.setInvoiceNumber(invoiceNo);
                            resp.setUid(uid);  // 設定uid
                            resp.setKey(key);
                            // 可能還有其他屬性要設定

                            createOrder(resp, "JKOPAY");
                        } else {
                            // 發票開立失敗，可看你要不要繼續 createOrder(沒有發票)、或是顯示錯誤
                            Toast.makeText(CheckoutActivity.this, "發票開立失敗", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            }
        }
    }

    private void handleCreditCardResponse(EcoPosResponse response, String orderNumber) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "Credit Card Status: " + status + ", Message: " + message);

            Toast.makeText(this, "Credit Card Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            
            if ("250".equals(status)) {  // 假設 "250" 代表成功
                Log.d(TAG, "信用卡交易成功，準備開立發票...");
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);



                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                new InvoiceTask(orderNumber) {
                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        // 等真正開立發票成功後再建立訂單
                        if ("OK".equals(result)) {
                            String invoiceNo = getInvoiceNumber();
                            String key = response.getKey();
                            String uid = response.getUid();
                            // 這裡拿到的 invoiceNumber 已經不再是 null
                            EcoPosResponse resp = new EcoPosResponse();
                            resp.setInvoiceNumber(invoiceNo);
                            resp.setUid(uid);  // 設定uid
                            resp.setKey(key);
                            // 可能還有其他屬性要設定

                            createOrder(resp, "CREDIT_CARD");
                        } else {
                            // 發票開立失敗，可看你要不要繼續 createOrder(沒有發票)、或是顯示錯誤
                            Toast.makeText(CheckoutActivity.this, "發票開立失敗", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            }
        }
    }

    private void handleScanResponse(EcoPosResponse response) {
        if (response != null) {
            String code = response.getCode();
            String message = response.getMsg();
            String result = response.getResult();
            Log.d(TAG, "Response: " + response);
            Log.d(TAG, "Scan Code: " + code + ", Message: " + message);

            Toast.makeText(this, "Scan Code: " + code + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            
            if ("200".equals(code)) {  // 假设 "200" 代表成功
                Log.d(TAG, "掃碼支付成功，準備開立發票...");
                Log.d(TAG, "Scan Result: " + result);
                carrierCode = result;  // 设置载具码
                if (carrierCode == null || carrierCode.isEmpty()) {
                    Toast.makeText(this, "Carrier code is empty!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Carrier code set: " + carrierCode, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItemsFromPreferences();
        if (cartAdapter != null) {
            cartAdapter.notifyDataSetChanged();  // 通知适配器数据更新
        }
    }
    private List<Item> mergeItems(List<Item> items) {
        List<Item> mergedItems = new ArrayList<>();
        for (Item item : items) {
            boolean found = false;
            for (Item mergedItem : mergedItems) {
                if (mergedItem.getId() == item.getId()) {
                    int newQuantity = Integer.parseInt(mergedItem.getQuantity()) + Integer.parseInt(item.getQuantity());
                    mergedItem.setQuantity(String.valueOf(newQuantity));
                    double newTotal = Double.parseDouble(mergedItem.getTotal()) + Double.parseDouble(item.getTotal());
                    mergedItem.setTotal(String.valueOf(newTotal));
                    found = true;
                    break;
                }
            }
            if (!found) {
                mergedItems.add(item);
            }
        }
        return mergedItems;
    }
    private OkHttpClient getHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void fetchOrderNumber(FetchOrderNumberCallback callback) {
        OkHttpClient client = getHttpClient();
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");

        RequestBody body = RequestBody.create("", null);
        Request request = new Request.Builder()
                .url(EnvironmentConfig.getOrderNumberUrl())
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch order number", e);
                runOnUiThread(() -> {
                    Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    callback.onFailure(e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body() != null ? response.body().string() : "{}";
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONObject dataObject = jsonObject.optJSONObject("data");
                        if (dataObject != null) {
                            fetchedOrderNumber = dataObject.optString("order_number", null);
                            runOnUiThread(() -> {
                                Toast.makeText(CheckoutActivity.this, "訂單編號取得成功: " + fetchedOrderNumber, Toast.LENGTH_LONG).show();
                                callback.onSuccess(fetchedOrderNumber);
                            });
                            Log.d(TAG, "Fetched Order Number: " + fetchedOrderNumber);
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(CheckoutActivity.this, "訂單編號取得失敗", Toast.LENGTH_LONG).show();
                                callback.onFailure("訂單編號取得失敗");
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing order number response", e);
                        runOnUiThread(() -> callback.onFailure("解析訂單編號失敗"));
                    }
                } else {
                    Log.e(TAG, "Failed to fetch order number, Response code: " + response.code());
                    runOnUiThread(() -> callback.onFailure("無法取得訂單編號，響應碼：" + response.code()));
                }
            }
        });
    }

    // 定義回呼介面
    interface FetchOrderNumberCallback {
        void onSuccess(String orderNumber);
        void onFailure(String errorMessage);
    }

    private void createOrder(EcoPosResponse response, String paymentType) {
        try {
            OkHttpClient client = getHttpClient();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");

            SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
            String token = sharedPreferences.getString("jwt_token", "");

            // 从Intent获取结账金额
            double totalAmountFromCart = getIntent().getDoubleExtra("totalAmount", 0);
            Log.d(TAG, "Total Amount from Cart: " + totalAmountFromCart);

            JSONObject orderData = new JSONObject();
            try {
                orderData.put("invoice_status", "COMPLETED");
                orderData.put("payment_type", paymentType);
                orderData.put("key", response.getKey());
                orderData.put("uid", response.getUid());
                orderData.put("order_number", fetchedOrderNumber);

                // 如果发票金额为空，使用结账金额
                double invoiceAmount = response.getInvoiceAmount() != null && response.getInvoiceAmount() > 0
                        ? response.getInvoiceAmount()
                        : totalAmountFromCart;
                double taxAmount;

                if (response.getB2bId() != null && !response.getB2bId().isEmpty()) {
                    taxAmount = Math.round(invoiceAmount * 0.05); // 假设税率为5%
                    orderData.put("tax_id_number", response.getB2bId());
                } else {
                    taxAmount = 0; // 没有发票信息时税额为0
                    orderData.put("tax_id_number", "");
                }

                // 根据发票类型设置 invoice_type 和相关字段
                if (carrierCode != null && !carrierCode.trim().isEmpty()) {
                    // 使用载具
                    orderData.put("invoice_type", "carrier");
                    orderData.put("carrier_number", carrierCode);
                } else if (loveCode != null && !loveCode.trim().isEmpty()) {
                    // 捐赠发票
                    orderData.put("invoice_type", "donation");
                    orderData.put("love_code", loveCode);
                } else {
                    // 默认实体发票
                    orderData.put("invoice_type", "physical");
                    orderData.put("carrier_number", "");
                }

                // 使用修正后的发票金额和税额
                orderData.put("invoice_number", (response.getInvoiceNumber() != null ? response.getInvoiceNumber() : "N/A"));
                orderData.put("invoice_amount", invoiceAmount);
                orderData.put("tax_amount", taxAmount);
                orderData.put("total_amount_excluding_tax", Math.round(invoiceAmount - taxAmount));
                orderData.put("transaction_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                List<Item> mergedItems = mergeItems(itemList); // 合并相同商品
                JSONArray itemsArray = new JSONArray();
                for (Item item : mergedItems) {
                    JSONObject itemObject = new JSONObject();
                    itemObject.put("product_id", item.getId());
                    itemObject.put("quantity", Integer.parseInt(item.getQuantity())); // 转换为整数

                    // 获取折扣后的价格
                    double unitPrice = 0.0;
                    double originalPrice = 0.0;

                    try {
                        // 先检查是否为null再处理
                        if (item.getPrice() != null && !item.getPrice().trim().isEmpty()) {
                            unitPrice = Double.parseDouble(item.getPrice());
                        } else {
                            Log.e(TAG, "Price is null or empty for item: " + item.getName());
                        }

                        if (item.getOriginalPrice() != null && !item.getOriginalPrice().trim().isEmpty()) {
                            originalPrice = Double.parseDouble(item.getOriginalPrice());
                        } else {
                            Log.e(TAG, "Original Price is null or empty for item: " + item.getName() + "original: " + item.getOriginalPrice());
                        }

                        // 打印 originalPrice 和 unitPrice 的值到日志
                        Log.d(TAG, "Product ID: " + item.getId() + ", Original Price: " + originalPrice + ", Unit Price: " + unitPrice);

                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Number format exception for item: " + item.getName(), e);
                    }

                    // 计算单件商品的折扣金额
                    double unitDiscountAmount = originalPrice - unitPrice;

                    // 打印 unitDiscountAmount 的值到日志
                    Log.d(TAG, "Product ID: " + item.getId() + ", Unit Discount Amount: " + unitDiscountAmount);

                    int intUnitPrice = (int) Math.round(unitPrice);
                    int intUnitDiscountAmount = (int) Math.round(unitDiscountAmount);
                    int intUnitPriceExcludingTax = (int) Math.round(calculateUnitPriceExcludingTax(unitPrice));
                    int intUnitTaxAmount = (int) Math.round(calculateUnitTaxAmount(unitPrice));

                    itemObject.put("unit_price", intUnitPrice);
                    itemObject.put("unit_discount_amount", intUnitDiscountAmount);
                    itemObject.put("unit_price_excluding_tax", intUnitPriceExcludingTax);
                    itemObject.put("unit_tax_amount", intUnitTaxAmount);

                    /*
                    itemObject.put("unit_price", unitPrice);
                    itemObject.put("unit_discount_amount", unitDiscountAmount); // 使用计算出的折扣金额
                    itemObject.put("unit_price_excluding_tax", (int) calculateUnitPriceExcludingTax(unitPrice)); // 转换为整数
                    itemObject.put("unit_tax_amount", (int) calculateUnitTaxAmount(unitPrice)); // 转换为整数

                     */
                    itemsArray.put(itemObject);
                }
                orderData.put("items", itemsArray);

                String employeeNumber = editEmployeeNumber.getText().toString();
                orderData.put("admin_id", employeeNumber);
                
                String invoiceRemarks = editInvoiceRemarks.getText().toString();
                String dealerCode = editDealerCode.getText().toString();
                orderData.put("invoice_remarks", invoiceRemarks);
                orderData.put("dealer_code", dealerCode);

            } catch (JSONException e) {
                Log.e(TAG, "Error creating order data", e);
            }

            RequestBody body = RequestBody.create(orderData.toString(), JSON);
            Request request = new Request.Builder()
                    .url(EnvironmentConfig.getOrdersUrl())
                    .post(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Order creation failed", e);
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "未知錯誤";
                    runOnUiThread(() -> Toast.makeText(CheckoutActivity.this, "訂單創建失敗: " + errorMessage, Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String successMessage = response.body() != null ? response.body().string() : "訂單創建成功，但無回應內容";
                        Log.d(TAG, "Order created successfully: " + successMessage);
                        runOnUiThread(() -> {
                            Toast.makeText(CheckoutActivity.this, "訂單創建成功", Toast.LENGTH_LONG).show();
                            clearItemsAndReturnToHome();
                        });
                    } else {
                        // 讀取錯誤資訊
                        String errorBody;
                        try {
                            errorBody = response.body() != null ? response.body().string() : "無錯誤資訊";
                        } catch (IOException e) {
                            errorBody = "無法讀取錯誤回應";
                        }

                        Log.e(TAG, "Order creation failed with code: " + response.code());
                        Log.e(TAG, "Response body: " + errorBody);

                        String finalErrorBody = errorBody; // 需要final變量來在Lambda中使用
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    CheckoutActivity.this,
                                    "訂單創建失敗\n錯誤代碼：" + response.code() + "\n詳細錯誤：" + finalErrorBody,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }

            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in createOrder method", e);
            Toast.makeText(CheckoutActivity.this, "訂單創建過程中發生錯誤", Toast.LENGTH_LONG).show();
        }
    }

    private double calculateUnitPriceExcludingTax(double unitPrice) {
        // 實現計算不含稅單價的邏輯
        double taxRate = 0.05; // 假設稅率為5%
        return unitPrice / (1 + taxRate);
    }

    private double calculateUnitTaxAmount(double unitPrice) {
        // 實現計算單個商品稅額的邏輯
        double taxRate = 0.05; // 假設稅率為5%
        return unitPrice - calculateUnitPriceExcludingTax(unitPrice);
    }

    private void scanCode() {  // 掃碼回傳
        try{
            EcoPosRequest appRequest = new EcoPosRequest();

            if (itemList == null) {
                itemList = new ArrayList<>();
            }

            if (invoice == null) {
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setPrintMode("2");
            }
            appRequest.setItemList(itemList);
            appRequest.setInvoice(invoice);

            ActionDetails actionDetails = new ActionDetails();
            actionDetails.setAction(Constant.ACTION_SCAN_CODE);
            actionDetails.setData(getAppRequestJsonString(appRequest));  // 取得app request的JSON string

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            startActivityForResult(intent, SCAN_CODE);
        }catch (Exception e) {
            Log.e(TAG, "Failed to start scan code activity", e);
            Toast.makeText(this, "Failed to start scan code activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void printInvoice(
            String invoiceNumber,
            String barcode,
            String qrcodeLeft,
            String qrcodeRight,
            long invoiceTime,
            String randomNumber,
            int amount
    ) {
        if (sunmiPrinterService == null) {
            Toast.makeText(this, "打印服務未連線", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            sunmiPrinterService.enterPrinterBuffer(true);

            // 1. 打印 LOGO or 標題
            Bitmap originalLogoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_invoice_logo);
            Bitmap resizedLogoBitmap = Bitmap.createScaledBitmap(originalLogoBitmap, 300, 100, false);
            Bitmap grayLogoBitmap = toGrayscale(resizedLogoBitmap);
            sunmiPrinterService.printBitmap(grayLogoBitmap, null);

            sunmiPrinterService.setAlignment(1, null);
            sunmiPrinterService.setFontSize(46, null);
            sunmiPrinterService.printText("\n電子發票證明聯\n", null);

            // 2. 發票期別
            String yearMonthStr = convertBarcodeToYearMonth(barcode);
            sunmiPrinterService.printText(yearMonthStr, null);

            // 3. 發票號碼
            String formattedInvoiceNumber = formatInvoiceNumber(invoiceNumber);
            sunmiPrinterService.printText(
                    (formattedInvoiceNumber != null ? formattedInvoiceNumber : invoiceNumber) + "\n",
                    null
            );

            // 4. 日期、隨機碼、總計
            sunmiPrinterService.setFontSize(24, null);
            sunmiPrinterService.setAlignment(0, null);
            if (invoiceTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String formattedDate = sdf.format(new Date(invoiceTime * 1000L));
                sunmiPrinterService.printText(formattedDate + "\n", null);
            }
            sunmiPrinterService.printText("隨機碼：" + randomNumber + "   ", null);
            sunmiPrinterService.printText("總計：" + amount + "\n", null);

            // 5. 賣方 & 買方
            sunmiPrinterService.printText("賣方：" + SELLER_TAX_ID, null);
            if (uniformNumber != null && !uniformNumber.isEmpty() && !"0000000000".equals(uniformNumber)) {
                sunmiPrinterService.printText("  買方：" + uniformNumber, null);
            }
            sunmiPrinterService.printText("\n", null);

            // 條碼
            sunmiPrinterService.printBarCode(barcode, 4, 50, 2, 0, null);

            sunmiPrinterService.setAlignment(1, null);
            Bitmap twoQrBitmap = drawTwoQRCodesOnCanvas(qrcodeLeft, qrcodeRight, 200);
            if (twoQrBitmap != null) {
                sunmiPrinterService.printBitmap(twoQrBitmap, null);
            }

            sunmiPrinterService.printText("\n\n\n\n", null);

            // 檢查是否需要列印SVG收據
            if (checkBoxPrintReceipt.isChecked()) {
                printSvgReceipt();
            }

            sunmiPrinterService.commitPrinterBuffer();
        } catch (RemoteException e) {
            e.printStackTrace();
            Toast.makeText(this, "列印時出現異常", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public String convertBarcodeToYearMonth(String barcode) {
        if (barcode != null && barcode.length() >= 5) {
            String barcodePrefix = barcode.substring(0, 5);
            Log.d("DEBUG", "Barcode Prefix: " + barcodePrefix); // Android日志打印

            String yearPart = barcodePrefix.substring(0, 3); // 获取年份部分
            String monthPart = barcodePrefix.substring(3, 5); // 获取月份区间标识部分

            int year = Integer.parseInt(yearPart);
            int monthIndicator = Integer.parseInt(monthPart);

            // 将月份区间标识转换为实际月份范围
            int startMonth = ((monthIndicator - 1) / 2) * 2 + 1;
            int endMonth = startMonth + 1;
            System.out.println("startMonth"+startMonth);
            System.out.println("endMonth"+endMonth);

            // 生成对应的年份和月份范围字符串
            return String.format("%d年%02d－%02d月\n", year, startMonth, endMonth);
        } else {
            Log.e("ERROR", "Invalid or short barcode received: " + barcode); // 打印错误日志
            return "error";
        }

    }

    private String formatInvoiceNumber(String invoiceNumber) {
        // 检查invoiceNumber是否符合预期的格式
        if (invoiceNumber != null && invoiceNumber.length() == 10 && invoiceNumber.substring(0, 2).matches("[a-zA-Z]+") && invoiceNumber.substring(2).matches("\\d+")) {
            // 在英文字母后加上'-'符号
            return invoiceNumber.substring(0, 2) + "-" + invoiceNumber.substring(2);
        } else {
            // 如果invoiceNumber不符合预期格式，返回原字符串或其他处理方式
            return invoiceNumber;
        }
    }

    private Bitmap drawTwoQRCodesOnCanvas(String qrText1, String qrText2, int qrSize) {
        int canvasWidth = qrSize * 2; // 假设Canvas的宽度是两个QR码宽度的总和
        int canvasHeight = qrSize; // Canvas的高度等于一个QR码的高度

        Bitmap combinedBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(combinedBitmap);

        Bitmap qrBitmap1 = generateQRCodeBitmap(qrText1, qrSize, qrSize);
        Bitmap qrBitmap2 = generateQRCodeBitmap(qrText2, qrSize+10, qrSize+10);

        // 在Canvas上绘制第一个QR码
        if (qrBitmap1 != null) {
            canvas.drawBitmap(qrBitmap1, 0, 0, null);
        }

        // 在Canvas上绘制第二个QR码，紧跟在第一个QR码的右边
        if (qrBitmap2 != null) {
            canvas.drawBitmap(qrBitmap2, qrSize, 0, null); // 假设两个QR码之间没有间距
        }

        //saveBitmapToFile(this, combinedBitmap, "myImage.png");


        return combinedBitmap;
    }

    private Bitmap generateQRCodeBitmap(String text, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class InvoiceTask extends AsyncTask<Void, Void, String> {


        // 從 API 取得的訂單編號
        private final String fetchedOrderNumber;

        public InvoiceTask(String orderNumber) {
            this.fetchedOrderNumber = orderNumber;
        }

        // 從開立發票 API 拿到的結果
        private String invoiceNumber = "";
        private long invoiceTime;
        private String randomNumber;
        private String barcode;
        private String qrcodeLeft;
        private String qrcodeRight;
        private String base64Data;
        private String msg; // API message

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // (A) 先「同步」取得 orderNumber
                if (fetchedOrderNumber == null || fetchedOrderNumber.isEmpty()) {
                    // 若取不到就直接回傳錯誤
                    return "Error: 無法取得 orderNumber";
                }

                // (B) 用 fetchedOrderNumber 做為發票的 OrderId
                // 1. 計算購物車原始總額（使用原始價格）
                double cartTotal = 0.0;
                for (Item item : itemList) {
                    double originalPrice = 0.0;
                    int quantity = 0;
                    try {
                        // 使用原始價格計算總額
                        if (item.getOriginalPrice() != null && !item.getOriginalPrice().trim().isEmpty()) {
                            originalPrice = Double.parseDouble(item.getOriginalPrice());
                        } else {
                            originalPrice = Double.parseDouble(item.getPrice());
                        }
                        quantity = Integer.parseInt(item.getQuantity());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    cartTotal += originalPrice * quantity;
                }

                // 2. 折扣 & 抵用
                double totalDiscount = discountAmount + rebateAmount;
                double finalAmount = cartTotal - totalDiscount;
                int intFinalAmount = (int) Math.round(finalAmount);

                // 3. 判斷是否含稅
                double salesAmount;
                double taxAmount;
                if (!"0000000000".equals(uniformNumber)) {
                    // 統編 => 假設含稅，稅率5%
                    //salesAmount = intFinalAmount / 1.05;
                    //taxAmount = intFinalAmount - salesAmount;
                    //salesAmount = intFinalAmount;
                    taxAmount = 0;
                } else {
                    // 一般 => 不分稅
                    //salesAmount = intFinalAmount;
                    //taxAmount = 0;
                    //salesAmount = intFinalAmount;
                    taxAmount = 0;
                }
                //int intSalesAmount = (int) Math.round(salesAmount);
                int intTaxAmount = (int) Math.round(taxAmount);

                // 4. 組出 ProductItem
                JSONArray productArray = new JSONArray();

                // 使用折讓後的總金額作為發票金額，而不是重新計算
                // int intFinalAmount = 0; // 移除這行，改用上面計算好的 intFinalAmount

                // 計算總的原始金額，用於檢查
                int originalTotalAmount = 0;
                int adjustedTotalAmount = 0;
                
                // 計算折讓比例：實際收款金額 / 原始總金額
                double discountRatio = cartTotal > 0 ? finalAmount / cartTotal : 1.0;
                
                Log.d("InvoiceDebug", "計算折讓比例: finalAmount=" + finalAmount + ", cartTotal=" + cartTotal + ", ratio=" + String.format("%.4f", discountRatio));
                
                for (Item cartItem : itemList) {
                    JSONObject productObj = new JSONObject();
                    productObj.put("Description", cartItem.getName());
                    productObj.put("Quantity", cartItem.getQuantity());
                    productObj.put("Remark", "");
                    productObj.put("TaxType", "3");  // 假設應稅

                    // 使用原始價格計算發票商品明細
                    double originalPrice = 0.0;
                    double currentPrice = 0.0;
                    int quantity = 0;
                    try {
                        // 優先使用原始價格，如果沒有則使用當前價格
                        if (cartItem.getOriginalPrice() != null && !cartItem.getOriginalPrice().trim().isEmpty()) {
                            originalPrice = Double.parseDouble(cartItem.getOriginalPrice());
                        } else {
                            originalPrice = Double.parseDouble(cartItem.getPrice());
                        }
                        currentPrice = Double.parseDouble(cartItem.getPrice());
                        quantity = Integer.parseInt(cartItem.getQuantity());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    // 計算調整後的金額（按折讓比例）
                    int intOriginalSubtotal = (int) Math.round(originalPrice * quantity);
                    int intAdjustedSubtotal = (int) Math.round(originalPrice * quantity * discountRatio);
                    int intAdjustedPrice = quantity > 0 ? (int) Math.round(intAdjustedSubtotal / (double) quantity) : 0;

                    productObj.put("UnitPrice", intAdjustedPrice);
                    productObj.put("Amount", intAdjustedSubtotal);
                    productArray.put(productObj);
                    originalTotalAmount += intOriginalSubtotal;
                    adjustedTotalAmount += intAdjustedSubtotal;
                    
                    Log.d("InvoiceDebug", "Item: " + cartItem.getName() + 
                          ", Original Price: " + originalPrice + 
                          ", Adjusted Price: " + intAdjustedPrice + 
                          ", Quantity: " + quantity + 
                          ", Original Amount: " + intOriginalSubtotal +
                          ", Adjusted Amount: " + intAdjustedSubtotal);
                }
                
                Log.d("InvoiceDebug", "=== 發票金額計算 ===");
                Log.d("InvoiceDebug", "原始商品明細總額: " + originalTotalAmount);
                Log.d("InvoiceDebug", "調整後商品明細總額 (ProductItem Amount 加總): " + adjustedTotalAmount);
                Log.d("InvoiceDebug", "FreeTaxSalesAmount: " + intFinalAmount);
                Log.d("InvoiceDebug", "TotalAmount: " + intFinalAmount);
                Log.d("InvoiceDebug", "TaxAmount: " + intTaxAmount);
                Log.d("InvoiceDebug", "折讓比例: " + String.format("%.4f", discountRatio));
                Log.d("InvoiceDebug", "驗算 TotalAmount = FreeTaxSalesAmount + TaxAmount: " + intFinalAmount + " = " + intFinalAmount + " + " + intTaxAmount);
                Log.d("InvoiceDebug", "商品明細總額與 FreeTaxSalesAmount 差異: " + (adjustedTotalAmount - intFinalAmount));

                // 5. 組發票 JSON
                JSONObject invoiceJson = new JSONObject();
                invoiceJson.put("OrderId", fetchedOrderNumber);   // 改用後端給的 orderNumber
                invoiceJson.put("BuyerIdentifier", uniformNumber);
                invoiceJson.put("BuyerName", "客人");
                invoiceJson.put("NPOBAN", "");
                invoiceJson.put("ProductItem", productArray);

                invoiceJson.put("SalesAmount", 0);
                invoiceJson.put("FreeTaxSalesAmount", intFinalAmount);  // 使用折讓後的實際金額，與 TotalAmount 一致
                invoiceJson.put("ZeroTaxSalesAmount", 0);
                invoiceJson.put("TaxType", "3");
                invoiceJson.put("TaxRate", "0");
                invoiceJson.put("TaxAmount", intTaxAmount);
                invoiceJson.put("TotalAmount", intFinalAmount);  // TotalAmount = FreeTaxSalesAmount + TaxAmount

                // 載具
                if (carrierCode != null && !carrierCode.isEmpty()) {
                    invoiceJson.put("CarrierType", "3J0002");
                    invoiceJson.put("CarrierId1", carrierCode);
                    invoiceJson.put("CarrierId2", carrierCode);
                }
                // 愛心碼
                if (loveCode != null && !loveCode.isEmpty()) {
                    invoiceJson.put("NPOBAN", loveCode);
                }

                // 6. 發票 API 簽名 / POST
                long currentTime = System.currentTimeMillis() / 1000;
                String apiData = invoiceJson.toString();
                String signature = md5(apiData + currentTime + APP_KEY);

                String postData = "invoice="+SELLER_TAX_ID + // 請改成你的值
                        "&data=" + URLEncoder.encode(apiData, "UTF-8") +
                        "&time=" + currentTime +
                        "&sign=" + signature;

                Log.d("DEBUG", "postData = " + postData);
                String response = performPostCall(INVOICE_URL, postData);
                Log.d("InvoiceTask", "performPostCall raw response => " + response);
                if (response == null) {
                    return "Error: 開立發票API無回應";
                }

                // 7. 處理回應
                JSONObject jsonResponse = new JSONObject(response);
                int code = jsonResponse.optInt("code", -1);
                msg = jsonResponse.optString("msg", "No msg");
                if (code == 0) {
                    invoiceNumber = jsonResponse.optString("invoice_number");
                    // 同時設定外部類別的發票號碼供列印使用
                    CheckoutActivity.this.invoiceNumber = invoiceNumber;
                    invoiceTime = jsonResponse.optLong("invoice_time");
                    randomNumber = jsonResponse.optString("random_number");
                    barcode = jsonResponse.optString("barcode");
                    qrcodeLeft = jsonResponse.optString("qrcode_left");
                    qrcodeRight = jsonResponse.optString("qrcode_right");
                    base64Data = jsonResponse.optString("base64_data");
                    return "OK";
                } else {
                    return "Error: " + msg;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("InvoiceTask", "onPostExecute called with result: " + result);
            
            if ("OK".equals(result)) {
                double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
                // 開立成功
                Toast.makeText(CheckoutActivity.this,
                        "發票開立成功，號碼：" + invoiceNumber,
                        Toast.LENGTH_LONG).show();

                // (選擇性) 列印
                // 檢查是否有載具，如果有載具則不列印發票

                boolean hasCarrier = carrierCode != null && !carrierCode.isEmpty();
                boolean hasLoveCode = loveCode != null && !loveCode.isEmpty();
                
                Log.d("InvoiceTask", "hasCarrier: " + hasCarrier + ", carrierCode: " + carrierCode);
                Log.d("InvoiceTask", "hasLoveCode: " + hasLoveCode + ", loveCode: " + loveCode);
                Log.d("InvoiceTask", "checkBoxPrintReceipt.isChecked(): " + checkBoxPrintReceipt.isChecked());

                if (!hasCarrier && !hasLoveCode) {
                    Log.d("InvoiceTask", "開始列印完整收據：發票 → SVG → 商品明細");
                    // 列印完整收據：發票 → SVG → 商品明細
                    printReceiptWithInvoiceData(
                            invoiceNumber,
                            barcode,
                            qrcodeLeft,
                            qrcodeRight,
                            invoiceTime,
                            randomNumber,
                            (int) Math.round(Double.parseDouble(String.valueOf(totalAmount)))
                    );
                } else {
                    Log.d("InvoiceTask", "載具存在，列印基本收據: " + carrierCode);
                    // 即使有載具，還是要列印收據（只是不包含發票資訊）
                    printReceipt();
                }
            } else {
                Log.e("InvoiceTask", "發票開立失敗，result: " + result);
                Toast.makeText(CheckoutActivity.this, "發票開立失敗: " + result, Toast.LENGTH_LONG).show();
            }

            // 重置載具、愛心碼、統編
            carrierCode = "";
            loveCode = "";
            uniformNumber = "0000000000";
        }

        /**
         * 同步取得後端 orderNumber
         */
        private String fetchOrderNumberSynchronously() {
            // 從 SharedPreferences 取 token
            SharedPreferences sp = getSharedPreferences("appPrefs", MODE_PRIVATE);
            String token = sp.getString("jwt_token", "");
            if (token == null || token.isEmpty()) {
                Log.e("InvoiceTask", "jwt_token is empty!");
                return null;
            }

            // 準備同步 OkHttp 請求
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create("", null); // 空的 post
            Request request = new Request.Builder()
                    .url(EnvironmentConfig.getOrderNumberUrl())
                    .post(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String respString = response.body() != null ? response.body().string() : "{}";
                    JSONObject jsonObject = new JSONObject(respString);
                    JSONObject dataObject = jsonObject.optJSONObject("data");
                    if (dataObject != null) {
                        String orderNumber = dataObject.optString("order_number", "");
                        Log.d("InvoiceTask", "Fetched orderNumber = " + orderNumber);
                        return orderNumber;
                    }
                } else {
                    Log.e("InvoiceTask", "fetchOrderNumber fail, code=" + response.code());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 同步 POST
         */
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

        /**
         * 計算 MD5
         */
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

    private void printSvgReceipt() {
        try {
            // 1. 檢查 SVG 檔案是否存在
            InputStream inputStream;
            try {
                inputStream = getAssets().open("contract_rotated.svg");
            } catch (IOException e) {
                Log.d(TAG, "SVG 檔案不存在，跳過 SVG 收據列印");
                return;
            }
            
            // 2. 讀取 SVG
            SVG svg = SVG.getFromInputStream(inputStream);

            // 3. 設定寬高（依你的紙張而定，80mm 印表機通常為 576px）
            int width = 390;
            int height = 1120;

            // 4. 建立原始 Bitmap 並填入白色背景（透明會導致全黑）
            Bitmap colored = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(colored);
            canvas.drawColor(Color.WHITE); // 避免透明
            svg.setDocumentWidth(width);
            svg.setDocumentHeight(height);
            svg.renderToCanvas(canvas);

            // 5. 將原始圖轉為黑白 Bitmap（避免灰階誤印）
            Bitmap bw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int pixel = colored.getPixel(x, y);
                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);
                    int gray = (r + g + b) / 3;
                    int bwPixel = gray < 160 ? Color.BLACK : Color.WHITE; // 可調整閾值
                    bw.setPixel(x, y, bwPixel);
                }
            }

            // 6. 傳給 Sunmi SDK 印出黑白 Bitmap
            sunmiPrinterService.printBitmap(bw, null);

        } catch (Exception e) {

            Log.e(TAG, "列印 SVG 收據失敗: " + e.getMessage(), e);
            Toast.makeText(this, "列印 SVG 收據失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void printReceipt() {
        printReceiptWithInvoiceData(null, null, null, null, 0, null, 0);
    }
    
    // 重載方法，支援發票資料
    private void printReceiptWithInvoiceData(String invoiceNum, String barcode, String qrcodeLeft, String qrcodeRight, long invoiceTime, String randomNumber, int totalAmount) {
        Log.d(TAG, "printReceiptWithInvoiceData called with invoiceNum: " + invoiceNum);
        try {
            if (sunmiPrinterService == null) {
                Log.e(TAG, "Sunmi 印表機服務未初始化");
                Toast.makeText(this, "印表機服務未初始化", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 判斷是否需要列印任何內容
            boolean shouldPrintInvoice = (invoiceNum != null && !invoiceNum.isEmpty());
            boolean shouldPrintReceipt = checkBoxPrintReceipt.isChecked();
            
            // 如果既沒有發票也沒勾選收據，則不列印任何東西
            if (!shouldPrintInvoice && !shouldPrintReceipt) {
                Log.d(TAG, "既沒有發票也未勾選收據，跳過列印");
                Toast.makeText(this, "沒有任何內容需要列印", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "開始列印 - 發票:" + shouldPrintInvoice + ", 收據:" + shouldPrintReceipt);
            
            // 開始印表機緩衝區
            sunmiPrinterService.enterPrinterBuffer(true);

            // === 第一部分：列印發票（如果有發票資料的話） ===
            if (invoiceNum != null && !invoiceNum.isEmpty()) {
                Log.d(TAG, "開始列印發票部分");
                printInvoiceSection(invoiceNum, barcode, qrcodeLeft, qrcodeRight, invoiceTime, randomNumber, totalAmount);
                sunmiPrinterService.printText("\n", null); // 分隔空行
            } else {
                // 如果沒有發票資料，列印基本發票資訊
                sunmiPrinterService.setAlignment(1, null); // 置中對齊
                sunmiPrinterService.printText("EcoPOS 收據\n", null);
                sunmiPrinterService.printText("========================\n", null);
                
                // 列印時間
                sunmiPrinterService.setAlignment(0, null); // 左對齊
                String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                sunmiPrinterService.printText("時間: " + currentTime + "\n", null);
                
                // 列印訂單號碼（如果有的話）
                if (fetchedOrderNumber != null && !fetchedOrderNumber.isEmpty()) {
                    sunmiPrinterService.printText("訂單號碼: " + fetchedOrderNumber + "\n", null);
                }
                
                sunmiPrinterService.printText("========================\n", null);
                sunmiPrinterService.printText("\n", null); // 分隔空行
            }
            
            // === 第二部分：只有在勾選收據時才列印 SVG 圖片 ===
            if (shouldPrintReceipt) {
                Log.d(TAG, "開始列印 SVG 圖片");
                printSvgImage();
                sunmiPrinterService.printText("\n", null); // 分隔空行
            }
                
            // === 第三部分：商品明細 - 總是列印 ===
            sunmiPrinterService.setFontSize(26, null);
            sunmiPrinterService.setAlignment(1, null); // 置中對齊
            sunmiPrinterService.printText("商品明細\n", null);
            sunmiPrinterService.printText("-----------------------------\n", null);
            
            // 列印商品清單
            if (cartItems != null && !cartItems.isEmpty()) {
                sunmiPrinterService.setAlignment(0, null); // 左對齊
                double subtotal = 0;
                
                for (CartItem item : cartItems) {
                    String itemName = item.getName();
                    int quantity = item.getQuantity();
                    double price = item.getPrice();
                    double itemTotal = quantity * price;
                    subtotal += itemTotal;
                    
                    // 商品名稱
                    sunmiPrinterService.printText(itemName + "\n", null);
                    // 數量和價格
                    sunmiPrinterService.printText(String.format(Locale.getDefault(), "  %d x $%.0f = $%.0f\n", 
                        quantity, price, itemTotal), null);
                }
                
                sunmiPrinterService.printText("-----------------------------\n", null);
                sunmiPrinterService.printText(String.format(Locale.getDefault(), "小計: $%.0f\n", subtotal), null);
                
                String totalText = editTextTotal.getText().toString();
                if (!totalText.isEmpty()) {
                    sunmiPrinterService.printText(String.format("總計: $%s\n", totalText), null);
                }
            }
            
            // 加空行便於撕紙
            sunmiPrinterService.printText("\n\n\n", null);
            
            // 提交印表機緩衝區，實際執行列印
            sunmiPrinterService.commitPrinterBuffer();
            Log.d(TAG, "收據列印完成 - 順序：發票 → SVG → 商品明細");
            
        } catch (Exception e) {
            Log.e(TAG, "列印收據失敗: " + e.getMessage(), e);
            Toast.makeText(this, "列印收據失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // 發票部分的列印邏輯（從原本的 printInvoice 方法提取）
    private void printInvoiceSection(String invoiceNumber, String barcode, String qrcodeLeft, String qrcodeRight, long invoiceTime, String randomNumber, int totalAmount) {
        try {
            // 1. 打印 LOGO or 標題
            Bitmap originalLogoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_invoice_logo);
            Bitmap resizedLogoBitmap = Bitmap.createScaledBitmap(originalLogoBitmap, 300, 100, false);
            Bitmap grayLogoBitmap = toGrayscale(resizedLogoBitmap);
            sunmiPrinterService.printBitmap(grayLogoBitmap, null);

            sunmiPrinterService.setAlignment(1, null);
            sunmiPrinterService.setFontSize(46, null);
            sunmiPrinterService.printText("\n電子發票證明聯\n", null);

            // 2. 發票期別
            String yearMonthStr = convertBarcodeToYearMonth(barcode);
            sunmiPrinterService.printText(yearMonthStr, null);

            // 3. 發票號碼
            String formattedInvoiceNumber = formatInvoiceNumber(invoiceNumber);
            sunmiPrinterService.printText(
                    (formattedInvoiceNumber != null ? formattedInvoiceNumber : invoiceNumber) + "\n",
                    null
            );

            // 4. 日期、隨機碼、總計
            sunmiPrinterService.setFontSize(24, null);
            sunmiPrinterService.setAlignment(0, null);
            if (invoiceTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String formattedDate = sdf.format(new Date(invoiceTime * 1000L));
                sunmiPrinterService.printText(formattedDate + "\n", null);
            }
            sunmiPrinterService.printText("隨機碼：" + randomNumber + "   ", null);
            sunmiPrinterService.printText("總計：" + totalAmount + "\n", null);

            // 5. 賣方 & 買方
            sunmiPrinterService.printText("賣方：" + SELLER_TAX_ID, null);
            if (uniformNumber != null && !uniformNumber.isEmpty() && !"0000000000".equals(uniformNumber)) {
                sunmiPrinterService.printText("  買方：" + uniformNumber, null);
            }
            sunmiPrinterService.printText("\n", null);

            // 條碼
            sunmiPrinterService.printBarCode(barcode, 4, 50, 2, 0, null);

            sunmiPrinterService.setAlignment(1, null);
            Bitmap twoQrBitmap = drawTwoQRCodesOnCanvas(qrcodeLeft, qrcodeRight, 200);
            if (twoQrBitmap != null) {
                sunmiPrinterService.printBitmap(twoQrBitmap, null);
            }

            sunmiPrinterService.printText("\n\n", null);
            
        } catch (Exception e) {
            Log.e(TAG, "列印發票部分失敗: " + e.getMessage(), e);
        }
    }
    
    // 獨立的 SVG 列印方法
    private void printSvgImage() {
        try {
            // 檢查 SVG 檔案是否存在
            InputStream inputStream;
            try {
                inputStream = getAssets().open("contract_rotated.svg");
            } catch (IOException e) {
                Log.w(TAG, "SVG 檔案不存在，跳過 SVG 列印: " + e.getMessage());
                return;
            }
            
            // 讀取SVG內容並轉換為bitmap後列印
            SVG svg = SVG.getFromInputStream(inputStream);
            if (svg != null) {
                // 設置SVG尺寸
                svg.setDocumentWidth(390); // 設定寬度為印表機寬度
                svg.setDocumentHeight(1120); // 設定適當高度
                
                // 創建bitmap
                Bitmap bitmap = Bitmap.createBitmap(390, 1120, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(android.graphics.Color.WHITE);
                svg.renderToCanvas(canvas);
                
                // 列印bitmap
                sunmiPrinterService.printBitmap(bitmap, null);
                Log.d(TAG, "SVG 列印成功");
            }
            
            inputStream.close();
            
        } catch (Exception e) {
            Log.e(TAG, "列印 SVG 失敗: " + e.getMessage(), e);
        }
    }

}
