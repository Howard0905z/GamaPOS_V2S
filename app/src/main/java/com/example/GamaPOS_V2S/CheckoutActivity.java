package com.example.GamaPOS_V2S;
import print.Print;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import static com.esc.PrinterHelper.PrintText;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

public class CheckoutActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String PACKAGE_NAME = "tw.com.mypay.tap.dev"; //測試
    //private static final String PACKAGE_NAME = "tw.com.mypay.tap"; //正式
    private static final String TARGET_ACTIVITY_NAME = "tw.com.mypay.MainActivity";

    private static final int LINE_PAY_REQUEST_CODE = 1;
    private static final int CREDIT_CARD_REQUEST_CODE = 2;
    private static final int CASH_REQUEST_CODE = 3;
    private static final int SCAN_CODE = 4;
    private static final int LINE_PAY_OFFLINE_CODE = 5;

    private static final int STORE_CREDIT_CARD_CODE = 6;
    private static final int GAMA_PAY_CODE = 7;
    private static final int CULTURE_COIN_CODE = 8;

    private EditText editTextTotal, editTextNumber;
    private TextView textViewChange;
    private EditText currentEditText;
    private LinearLayout buttonContainer;

    private SunmiPrinterService sunmiPrinterService = null;

    private static int orderCounter = 0;

    private String currentOrderId;
    private static String lastOrderTime = "";

    private static String taxID = "66294370";

    private String totalString;
    private int totalAmount;
    private String loveCode = "";
    private String uniformNumber = "0000000000";

    private String carrierCode = "";

    private List<Item> itemList;
    private ArrayList<CartItem> cartItems;
    private Invoice invoice;

    private Print Printer = new Print();
    private EditText edtIP = null;
    private EditText edtPort = null;
    private TextView txtTips = null;

    private String PrinterName = "";

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "OrderNumberPrefs";
    private static final String KEY_ORDER_NUMBER = "OrderNumber";

    private CartAdapter cartAdapter; // 声明 CartAdapter

    private String fetchedOrderNumber = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
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

        Button buttonCash = findViewById(R.id.buttonCash);
        Button buttonLinePay = findViewById(R.id.buttonLinePay);
        Button buttonCreditCard = findViewById(R.id.buttonCreditCard);
        Button buttonStoreCreditCard = findViewById(R.id.buttonStoreCreditCard);
        Button buttonGamaPay = findViewById(R.id.buttonGamaPay);
        Button buttonCultureCoin = findViewById(R.id.buttonCultureCoin);

        Button buttonCreditCardReceived = findViewById(R.id.buttonCreditCardReceived);
        Button buttonCashReceived = findViewById(R.id.buttonCashReceived);
        Button buttonLinePayReceived = findViewById(R.id.buttonLinePayReceived);
        Button buttonShoppingMallGiftCardReceived = findViewById(R.id.buttonShoppingMallGiftCardReceived);
        Button buttonCultureCoinReceived = findViewById(R.id.buttonCultureCoinReceived);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("jwt_token", "");
        OkHttpClient client = new OkHttpClient();

        // 建立 Request
        Request request = new Request.Builder()
                .url("http://172.207.27.24:8100/user/payment_methods")
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
                        final boolean gamaPayEnabled = dataObject.optBoolean("GAMA_PAY", false);
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
                                    buttonGamaPay.setVisibility(gamaPayEnabled ? View.VISIBLE : View.GONE);
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
                                    if (gamaPayEnabled) buttonContainer.addView(buttonGamaPay);
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

        buttonCash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOrderNumber(new FetchOrderNumberCallback() {
                    @Override
                    public void onSuccess(String orderNumber) {
                        sendCashTransaction(); // 訂單編號成功獲取後執行結帳
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch order number: " + errorMessage);
                        Toast.makeText(CheckoutActivity.this, "無法取得訂單編號: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });


        buttonGamaPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectInvoiceData();
                sendGamaPayTransaction();
            }
        });

        buttonStoreCreditCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectInvoiceData();
                sendStoreCreditCardTransaction();
            }
        });

        buttonLinePay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectInvoiceData();
                //sendScanQrCodeTransaction();
                sendOfflineTransaction();
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

        buttonCreditCardReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrder(new GamaPosResponse(), "CREDIT_CARD_RECEIVED");
            }
        });

        buttonCashReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrder(new GamaPosResponse(), "CASH_RECEIVED");
            }
        });

        buttonLinePayReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrder(new GamaPosResponse(), "LINEPAY_RECEIVED");
            }
        });

        buttonShoppingMallGiftCardReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrder(new GamaPosResponse(), "SHOPPING_MALL_GIFT_CARD_RECEIVED");
            }
        });

        buttonCultureCoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //collectInvoiceData();
                sendCultureCoinTransaction();
            }
        });

        buttonCultureCoinReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrder(new GamaPosResponse(), "CULTURE_COIN_RECEIVED");
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

    private void showCarrierInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("載具輸入");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("確認", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                carrierCode = input.getText().toString();
                Toast.makeText(getApplicationContext(), "載具碼: " + carrierCode, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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

    private void showInvoiceCancellationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("請輸入發票號碼");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("作廢", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String invoiceNumber = input.getText().toString();
                Toast.makeText(getApplicationContext(), "發票作廢: " + invoiceNumber, Toast.LENGTH_SHORT).show();
                // Handle invoice cancellation logic
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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

    private void sendScanQrCodeTransaction() {
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            String orderId = generateOrderId();


            GamaPosRequest appRequest = new GamaPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId(Constant.PAY_MODE_ID_LINE_PAY);
            appRequest.setItemList(itemList);
            appRequest.setInvoice(invoice);
            appRequest.getPrintOrderDetailsReceipt();
            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

            ActionDetails actionDetails = getTransactionActionDetails(appRequest);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);

            Log.d(TAG, "Starting LinePayActivityOnline with amount: " + totalAmount + " and orderId: " + orderId);

            startActivityForResult(intent, LINE_PAY_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LinePayActivity", e);
            Toast.makeText(this, "Failed to start LinePayActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendOfflineTransaction() {  // 所有線下交易，掃碼收款
        try {
        double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
        String orderId = generateOrderId();

        GamaPosRequest appRequest = new GamaPosRequest();
        appRequest.setAmount(totalAmount);
        appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
        appRequest.setPayModeId(Constant.BUY_39_PAY_MODE_ID_OFFLINE_ALL);
        appRequest.setItemList(itemList);
        appRequest.setInvoice(invoice);

            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

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

    private void sendCreditCardTransaction() {
        try {// 信用卡交易
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            String orderId = generateOrderId();

            GamaPosRequest appRequest = new GamaPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId(Constant.PAY_MODE_ID_CREDIT_CARD);
            appRequest.setItemList(itemList);
            appRequest.setInvoice(invoice);
            appRequest.setCreditCardReceiptType("1");

            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

            ActionDetails actionDetails = getTransactionActionDetails(appRequest);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            startActivityForResult(intent, CREDIT_CARD_REQUEST_CODE);
        }catch (Exception e) {
            Log.e(TAG, "Failed to start CreditCardActivity", e);
            Toast.makeText(this, "Failed to start CreditCardActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendCreditCardTapTransaction() {
        try {// 信用卡Tap交易
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            String orderId = generateOrderId();

            GamaPosRequest appRequest = new GamaPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId(Constant.PAY_MODE_ID_CREDIT_CARD);
            appRequest.setItemList(itemList);
            appRequest.setInvoice(invoice);
            appRequest.setCreditCardReceiptType("1");

            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

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

    private void sendCashTransaction() {
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            // 检查 fetchedOrderNumber 是否为空或未初始化
            if (fetchedOrderNumber == null || fetchedOrderNumber.isEmpty()) {
                Log.e(TAG, "Fetched Order Number is null or empty!");
                Toast.makeText(this, "訂單編號無效，請檢查", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d(TAG, "Fetched Order Number: " + fetchedOrderNumber); // 输出 fetchedOrderNumber 的值
            currentOrderId = generateOrderId(); // 存储生成的订单编号

            GamaPosRequest appRequest = new GamaPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId("37");

            if (itemList == null) {
                itemList = new ArrayList<>();
            }
            appRequest.setItemList(itemList);
            Log.d("Checkout", "itemList: " + itemList);

            // 检查并初始化 invoice
            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

            ActionDetails actionDetails = getTransactionActionDetails(appRequest);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            startActivityForResult(intent, CASH_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start CashActivity", e);
            Toast.makeText(this, "Failed to start CashActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendGamaPayTransaction() {
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            currentOrderId = generateOrderId(); // 存储生成的订单编号

            GamaPosRequest appRequest = new GamaPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId("37");

            if (itemList == null) {
                itemList = new ArrayList<>();
            }
            appRequest.setItemList(itemList);

            // 检查并初始化 invoice
            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

            ActionDetails actionDetails = getTransactionActionDetails(appRequest);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            startActivityForResult(intent, GAMA_PAY_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start CashActivity", e);
            Toast.makeText(this, "Failed to start CashActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendStoreCreditCardTransaction() {
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            currentOrderId = generateOrderId(); // 存储生成的订单编号

            GamaPosRequest appRequest = new GamaPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId("37");
            appRequest.setCreditCardReceiptType("1");

            if (itemList == null) {
                itemList = new ArrayList<>();
            }
            appRequest.setItemList(itemList);

            // 检查并初始化 invoice
            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

            ActionDetails actionDetails = getTransactionActionDetails(appRequest);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            startActivityForResult(intent, STORE_CREDIT_CARD_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start CashActivity", e);
            Toast.makeText(this, "Failed to start CashActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendCultureCoinTransaction() {
        try {
            double totalAmount = Double.parseDouble(editTextTotal.getText().toString());
            currentOrderId = generateOrderId(); // 存储生成的订单编号

            GamaPosRequest appRequest = new GamaPosRequest();
            appRequest.setAmount(totalAmount);
            appRequest.setOrderId(fetchedOrderNumber); // 使用存储的订单编号
            appRequest.setPayModeId("37");

            if (itemList == null) {
                itemList = new ArrayList<>();
            }
            appRequest.setItemList(itemList);

            // 检查并初始化 invoice
            if (carrierCode != null && !carrierCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "載具", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(1); // 设定输入类型为 1
                invoice.setCloudType(2); // 设定云端类型
                invoice.setMobileCode(carrierCode); // 设定载具码
                invoice.setIssuerState(1);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (loveCode != null && !loveCode.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "捐贈", Toast.LENGTH_LONG).show();
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setInputType(2);
                invoice.setIssuerState(1);
                invoice.setLoveCode(loveCode);
                invoice.setPrintMode("0"); // 设定打印模式
            } else if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(this, "發票模式: " + "統編", Toast.LENGTH_LONG).show();
                appRequest.setPrintOrderDetailsReceipt(true);
                invoice = new Invoice();
                invoice.setRateType(1);
                invoice.setBuyerIdId(uniformNumber);
                invoice.setInputType(3);
                invoice.setIssuerState(1);
                invoice.setB2bId(uniformNumber);
                invoice.setPrintMode("1"); // 设定打印模式
            } else {
                if (invoice == null) {
                    Toast.makeText(this, "發票模式: " + "一般", Toast.LENGTH_LONG).show();
                    invoice = new Invoice();
                    invoice.setRateType(1);
                    invoice.setInputType(3); // 实体发票
                    invoice.setIssuerState(1);
                    invoice.setPrintMode("2");
                }
            }
            appRequest.setInvoice(invoice);

            ActionDetails actionDetails = getTransactionActionDetails(appRequest);

            Intent intent = new Intent();
            intent.putExtra(Constant.INTENT_EXTRA_KEY_REQUEST_ACTION_DETAILS, actionDetails);
            intent.setClassName(PACKAGE_NAME, TARGET_ACTIVITY_NAME);
            startActivityForResult(intent, CULTURE_COIN_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start CashActivity", e);
            Toast.makeText(this, "Failed to start CashActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("fromCheckout", true); // 設置標誌
        startActivity(intent);
        finish();
    }

    private void refreshCartItems() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");
        Type listType = new TypeToken<ArrayList<CartItem>>() {}.getType();
        List<CartItem> updatedCartItems = new Gson().fromJson(cartItemsJson, listType);

        // 日志记录
        Log.d(TAG, "刷新后的購物車内容: " + cartItemsJson);

        // 更新購物車列表
        if (updatedCartItems != null) {
            cartItems.clear();
            cartItems.addAll(updatedCartItems);
        } else {
            cartItems.clear();
        }

        // 更新UI或适配器來反映購物車內容變化
        if (cartAdapter != null) {
            cartAdapter.notifyDataSetChanged();
        }
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

    private ActionDetails getTransactionActionDetails(GamaPosRequest appRequest) {
        ActionDetails actionDetails = new ActionDetails();
        actionDetails.setAction(Constant.ACTION_SEND_TRANSACTION);
        actionDetails.setData(getAppRequestJsonString(appRequest));
        return actionDetails;
    }

    private ActionDetails getTapTransactionActionDetails(GamaPosRequest appRequest) {
        ActionDetails actionDetails = new ActionDetails();
        actionDetails.setAction(Constant.ACTION_TAP);
        actionDetails.setData(getAppRequestJsonString(appRequest));
        return actionDetails;
    }

    private String getAppRequestJsonString(GamaPosRequest appRequest) {
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

            // Add invoice data to JSON
            JSONObject invoiceObject = new JSONObject();
            invoiceObject.put("rateType", String.valueOf(appRequest.getInvoice().getRateType()));
            invoiceObject.put("inputType", String.valueOf(appRequest.getInvoice().getInputType()));
            if (carrierCode != null && !carrierCode.isEmpty()) {
                invoiceObject.put("cloudType", String.valueOf(appRequest.getInvoice().getCloudType()));
                invoiceObject.put("mobileCode", appRequest.getInvoice().getMobileCode());
            }
            if (!loveCode.isEmpty()) {
                Toast.makeText(CheckoutActivity.this, "愛心碼: " + loveCode, Toast.LENGTH_LONG).show();
                invoiceObject.put("loveCode", appRequest.getInvoice().getLoveCode());
            }
            if (uniformNumber != "0000000000" && !uniformNumber.isEmpty()) {
                Toast.makeText(CheckoutActivity.this, "統編: " + uniformNumber, Toast.LENGTH_LONG).show();
                invoiceObject.put("b2bId", appRequest.getInvoice().getB2bId());
                invoiceObject.put("isPrintOrderDetailsReceipt", appRequest.getPrintOrderDetailsReceipt());
            }

            invoiceObject.put("issuerState", String.valueOf(appRequest.getInvoice().getIssuerState()));
            invoiceObject.put("printMode", String.valueOf(appRequest.getInvoice().getPrintMode()));

            jsonObject.put("invoice", invoiceObject);

            Log.d("getAppRequestJsonString", "Invoice Details: " + invoiceObject.toString());

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

        if ((requestCode == LINE_PAY_REQUEST_CODE || requestCode == CREDIT_CARD_REQUEST_CODE || requestCode == CASH_REQUEST_CODE || requestCode == SCAN_CODE || requestCode == STORE_CREDIT_CARD_CODE || requestCode == GAMA_PAY_CODE || requestCode == LINE_PAY_OFFLINE_CODE || requestCode == CULTURE_COIN_CODE) && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "Handling result for requestCode=" + requestCode);

            ActionDetails actionDetails = data.getParcelableExtra(Constant.INTENT_EXTRA_KEY_RESPONSE_ACTION_DETAILS);
            if (actionDetails != null) {
                String responseData = actionDetails.getData();
                Log.d(TAG, "Received action details data: " + responseData);

                GamaPosResponse appResponse = getAppResponse(responseData);  // 轉換為你的 app response

                // Log the uid and key
                Log.d(TAG, "UID: " + appResponse.getUid());
                Log.d(TAG, "Key: " + appResponse.getKey());

                // Use the uid and key as needed
                if (requestCode == LINE_PAY_REQUEST_CODE) {
                    handleLinePayResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode == CREDIT_CARD_REQUEST_CODE) {
                    handleCreditCardResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode == CASH_REQUEST_CODE) {
                    handleCashResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode == SCAN_CODE){
                    handleScanResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode == LINE_PAY_OFFLINE_CODE) {
                    handleLinePayOfflineResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode ==  GAMA_PAY_CODE) {
                    handleGamaPayResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode ==  STORE_CREDIT_CARD_CODE) {
                    handleStoreCreditCardResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
                } else if (requestCode ==  CULTURE_COIN_CODE) {
                    handleCultureCoinResponse(appResponse);
                    cartItems.clear(); // Clear the cart items
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
    private GamaPosResponse getAppResponse(String data) {
        GamaPosResponse response = new GamaPosResponse();
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

            // 獲取發票信息
            JSONObject invoiceJson = jsonObject.optJSONObject("invoice");
            if (invoiceJson != null) {
                response.setInvoiceAmount(invoiceJson.has("amount") && !invoiceJson.isNull("amount") ? invoiceJson.getDouble("amount") : null);
                response.setInvoiceNumber(invoiceJson.optString("number"));
                response.setWordTrack(invoiceJson.optString("wordTrack"));
                response.setTaxIdNumber(invoiceJson.optString("taxId"));
            }

            Log.d(TAG, "Parsed response: " + jsonObject.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse response JSON", e);
        }
        return response;
    }

    private void handleLinePayResponse(GamaPosResponse response) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "Line Pay Status: " + status + ", Message: " + message);

            Toast.makeText(this, "Line Pay Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();

            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);
                
                createOrder(response, "LINEPAY");

                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                //printReceipt();
            }
        }
    }

    private void handleLinePayOfflineResponse(GamaPosResponse response) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "Line Pay Offline Status: " + status + ", Message: " + message);

            Toast.makeText(this, "Line Pay Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();

            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);
                
                createOrder(response, "LINEPAY");

                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                //printReceipt();
            }
        }
    }

    private void handleCreditCardResponse(GamaPosResponse response) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "Credit Card Status: " + status + ", Message: " + message);

            Toast.makeText(this, "Credit Card Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "About to call printReceipt...");
            //printReceipt();
            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);
                
                createOrder(response, "CREDIT_CARD");
                Log.d(TAG, "调用 clearItemsAndReturnToHome 方法"); // 添加日誌記錄
                clearItemsAndReturnToHome(); // 確保在支付成功後清空購物車
                saveCartItemsToPreferences();
            }
        }
    }

    private void handleCashResponse(GamaPosResponse response) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "Cash Status: " + status + ", Message: " + message);

            Toast.makeText(this, "Cash Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "About to call printReceipt...");
            //printReceipt();
            if ("250".equals(status)) {  // 假设 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);
                
                createOrder(response, "CASH");

                Log.d(TAG, "調用 clearItemsAndReturnToHome 方法");
                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
            }
        }
    }

    private void handleGamaPayResponse(GamaPosResponse response) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "GamaPay Status: " + status + ", Message: " + message);

            Toast.makeText(this, "GamaPay Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "About to call printReceipt...");
            //printReceipt();
            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);
                
                createOrder(response, "GAMA_PAY");

                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                //printReceipt();
            }
        }
    }

    private void handleStoreCreditCardResponse(GamaPosResponse response) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "toreCreditCard Status: " + status + ", Message: " + message);

            Toast.makeText(this, "toreCreditCard Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "About to call printReceipt...");
            //printReceipt();
            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);
                
                createOrder(response, "SHOPPING_MALL_CREDIT_CARD");
                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                //printReceipt();
            }
        }
    }

    private void handleCultureCoinResponse(GamaPosResponse response) {
        if (response != null) {
            String status = response.getStatus();
            String message = response.getMessage();
            Log.d(TAG, "CultureCoin Status: " + status + ", Message: " + message);

            Toast.makeText(this, "CultureCoin Status: " + status + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "About to call printReceipt...");
            //printReceipt();
            if ("250".equals(status)) {  // 假設 "250" 代表成功
                // 假設您在某個按鈕點擊事件中調用 createOrder
                double discountAmount = getIntent().getDoubleExtra("discountAmount", 0);
                double rebateAmount = getIntent().getDoubleExtra("rebateAmount", 0);
                
                createOrder(response, "CULTURE_COIN");
                clearItemsAndReturnToHome();
                saveCartItemsToPreferences();
                //printReceipt();
            }
        }
    }

    private void handleScanResponse(GamaPosResponse response) {
        if (response != null) {
            String code = response.getCode();
            String message = response.getMsg();
            String result = response.getResult();
            Log.d(TAG, "Response: " + response);
            Log.d(TAG, "Scan Code: " + code + ", Message: " + message);

            Toast.makeText(this, "Scan Code: " + code + "\nMessage: " + message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "About to call printReceipt...");
            //printReceipt();
            if ("200".equals(code)) {  // 假设 "200" 代表成功
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
                .url("http://172.207.27.24:8100/orders/number")
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

    private void createOrder(GamaPosResponse response, String paymentType) {
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

                if (response.getTaxIdNumber() != null && !response.getTaxIdNumber().isEmpty()) {
                    taxAmount = Math.round(invoiceAmount * 0.05); // 假设税率为5%
                    orderData.put("tax_id_number", response.getTaxIdNumber());
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
                orderData.put("invoice_number", response.getWordTrack() + "-" + (response.getInvoiceNumber() != null ? response.getInvoiceNumber() : "N/A"));
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

                    itemObject.put("unit_price", unitPrice);
                    itemObject.put("unit_discount_amount", unitDiscountAmount); // 使用计算出的折扣金额
                    itemObject.put("unit_price_excluding_tax", (int) calculateUnitPriceExcludingTax(unitPrice)); // 转换为整数
                    itemObject.put("unit_tax_amount", (int) calculateUnitTaxAmount(unitPrice)); // 转换为整数
                    itemsArray.put(itemObject);
                }
                orderData.put("items", itemsArray);

            } catch (JSONException e) {
                Log.e(TAG, "Error creating order data", e);
            }

            RequestBody body = RequestBody.create(orderData.toString(), JSON);
            Request request = new Request.Builder()
                    .url("http://172.207.27.24:8100/orders")
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
            GamaPosRequest appRequest = new GamaPosRequest();

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
}
