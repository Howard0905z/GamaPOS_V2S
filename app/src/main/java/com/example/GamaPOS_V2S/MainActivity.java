package com.example.GamaPOS_V2S;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView textViewOrderSummary;
    private Button buttonNextStep;
    private LinearLayout layoutOrderSummary;
    private int orderCount = 0;

    private static final int STORAGE_PERMISSION_CODE = 101;

    private RecyclerView recyclerViewTop;
    private RecyclerView recyclerViewBottom;
    private RecyclerView recyclerViewButtons;
    private Button buttonScan;
    private EditText editTextScan;
    private ArrayList<CartItem> cartItems = new ArrayList<>();

    private CartAdapter cartAdapter; // 声明 CartAdapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        textViewOrderSummary = findViewById(R.id.textView_order_summary);
        layoutOrderSummary = findViewById(R.id.layout_order_summary);
        recyclerViewTop = findViewById(R.id.recyclerViewTop);
        recyclerViewBottom = findViewById(R.id.recyclerViewBottom);
        recyclerViewButtons = findViewById(R.id.recyclerViewButtons);
        buttonNextStep = findViewById(R.id.button_next_step);
        buttonScan = findViewById(R.id.button_scanner);

        Button buttonMain = findViewById(R.id.button_main);
        Button buttonRecord = findViewById(R.id.button_record);
        Button buttonCheckout = findViewById(R.id.button_checkout);
        Button buttonLogout = findViewById(R.id.button_logout);

        buttonNextStep.setOnClickListener(v -> goToNextStep());
        buttonMain.setOnClickListener(v -> goToMainActivity());
        buttonRecord.setOnClickListener(v -> goToTransactionRecordsActivity());
        buttonCheckout.setOnClickListener(v -> goToCheckoutActivity());
        buttonScan.setOnClickListener(v -> showScanDialog());
        buttonLogout.setOnClickListener(v -> logout());

        LinearLayoutManager layoutManagerTop = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        LinearLayoutManager layoutManagerBottom = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        GridLayoutManager layoutManagerButtons = new GridLayoutManager(this, 2); // 使用GridLayoutManager，每行兩個按鈕

        recyclerViewTop.setLayoutManager(layoutManagerTop);
        recyclerViewBottom.setLayoutManager(layoutManagerBottom);
        recyclerViewButtons.setLayoutManager(layoutManagerButtons);

        // Initialize with empty adapter to avoid "No adapter attached; skipping layout"
        recyclerViewTop.setAdapter(new CategoryAdapter(this, new ArrayList<>(), category -> onMainCategoryClicked((Category) category), false));
        recyclerViewBottom.setAdapter(new CategoryAdapter(this, new ArrayList<>(), subCategory -> onSubCategoryClicked((SubCategory) subCategory), false));
        recyclerViewButtons.setAdapter(new CategoryAdapter(this, new ArrayList<>(), null, true));

        if (checkAndRequestPermissions()) {
            fetchCategories();
        }

        // 初始化购物车列表和适配器
        loadCartItemsFromPreferences(); // 改為在這裡加載購物車數據
        Log.d("MainActivity", "Cart items loaded: " + cartItems.size());
        cartAdapter = new CartAdapter(this, cartItems, 80.0, 200.0); // 確保使用正確的構造函數
        RecyclerView recyclerViewCart = findViewById(R.id.recyclerViewCart);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCart.setAdapter(cartAdapter);
        Log.d("MainActivity", "CartAdapter initialized and set to RecyclerView");

        updateOrderSummary(cartItems.size());
    }




    private void showScanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_scan, null);
        builder.setView(view);

        editTextScan = view.findViewById(R.id.editTextScan);
        editTextScan.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || event.getAction() == KeyEvent.ACTION_DOWN) {
                String scannedCode = editTextScan.getText().toString();
                fetchProductBySKU(scannedCode);
                editTextScan.setText(""); // Clear the input field for the next scan
                return true;
            }
            return false;
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void fetchProductBySKU(String sku) {
        OkHttpClient client = new OkHttpClient();
        String token = getToken();
        String url = "http://172.207.27.24:8100/products/SKU/multiple?SKU=" + sku;

        Log.d(TAG, "Request URL: " + url);
        Log.d(TAG, "Authorization Token: " + token);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("accept", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network request failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "網路請求失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d(TAG, "API響應: " + responseBody);
                Log.d(TAG, "HTTP Status Code: " + response.code());
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API錯誤: " + responseBody, Toast.LENGTH_LONG).show());
                } else {
                    try {
                        JSONArray dataJsonArray = new JSONObject(responseBody).getJSONArray("data");

                        if (dataJsonArray.length() == 1) {
                            // 只有一個商品，直接加入購物車並顯示庫存
                            JSONObject dataJson = dataJsonArray.getJSONObject(0);
                            String productName = dataJson.getString("name");
                            int productId = dataJson.getInt("product_id");
                            int price = dataJson.getInt("price");
                            int quantity = dataJson.getInt("quantity");  // 取得庫存數量

                            // 提示產品加入購物車並顯示庫存數量
                            String message = productName + " 已加入購物車\n庫存: " + quantity;
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());

                            Product product = new Product(productId, productName, price, new ArrayList<>());
                            addToCart(product);
                        } else if (dataJsonArray.length() > 1) {
                            // 多個商品，顯示選擇視窗
                            runOnUiThread(() -> showProductSelectionDialog(dataJsonArray));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse product data", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "解析產品數據失敗", Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    private void showProductSelectionDialog(JSONArray products) {
        // 建立一個選擇器讓使用者選擇
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("選擇商品");

        List<String> productNames = new ArrayList<>();
        for (int i = 0; i < products.length(); i++) {
            try {
                productNames.add(products.getJSONObject(i).getString("name"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        builder.setItems(productNames.toArray(new String[0]), (dialog, which) -> {
            try {
                JSONObject selectedProduct = products.getJSONObject(which);
                String productName = selectedProduct.getString("name");
                int productId = selectedProduct.getInt("product_id");
                int price = selectedProduct.getInt("price");

                Product product = new Product(productId, productName, price, new ArrayList<>());
                addToCart(product);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, productName + " 已加入購物車", Toast.LENGTH_LONG).show());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        builder.show();
    }

    private void addToCart(Product product) {
        runOnUiThread(() -> {
            boolean itemExists = false;
            for (CartItem item : cartItems) {
                if (item.getId() == product.getProductId()) {
                    item.setQuantity(item.getQuantity() + 1);
                    itemExists = true;
                    break;
                }
            }
            if (!itemExists) {
                CartItem cartItem = new CartItem(product.getProductId(), product.getProductName(), product.getPrice(), 1, true);
                cartItems.add(cartItem);
            }
            cartAdapter.notifyDataSetChanged();
            saveCartItemsToPreferences();  // 保存更新后的购物车数据到 SharedPreferences
            updateOrderSummary(cartItems.size());
        });
    }



    private void saveCartItemsToPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cartItemsJsonBefore = new Gson().toJson(cartItems);
        Log.d(TAG, "saveCartItemsToPreferences: Before saving, cart items: " + cartItemsJsonBefore);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        String cartItemsJson = new Gson().toJson(cartItems);
        editor.putString("cartItems", cartItemsJson);
        editor.apply();

        Log.d(TAG, "saveCartItemsToPreferences: Saved cart items to preferences: " + cartItemsJson);
    }





    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showPermissionExplanationDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            }
            return false;
        }
        return true;
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("存儲權限需要")
                .setMessage("此應用需要存儲權限來存儲和訪問文件。請授予存儲權限。")
                .setPositiveButton("授予權限", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE))
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存儲權限已授予", Toast.LENGTH_SHORT).show();
                fetchCategories();
            } else {
                Toast.makeText(this, "存儲權限被拒絕", Toast.LENGTH_SHORT).show();
                fetchCategories(); // Call fetchCategories() even if permission is denied, but limit functionality if needed
            }
        }
    }

    private List<Category> allCategories = new ArrayList<>(); // 新增變量來存儲所有類別數據

    private void fetchCategories() {
        OkHttpClient client = new OkHttpClient();
        String token = getToken();
        Log.d(TAG, "使用的Token: " + token);
        Request request = new Request.Builder()
                .url("http://172.207.27.24:8100/categories")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "網絡請求失敗", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "網路請求失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d(TAG, "API響應: " + responseBody);
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API錯誤: " + responseBody, Toast.LENGTH_LONG).show());
                } else {
                    Type responseType = new TypeToken<ResponseWrapper<List<Category>>>(){}.getType();
                    ResponseWrapper<List<Category>> responseWrapper = new Gson().fromJson(responseBody, responseType);
                    List<Category> categories = responseWrapper.getData();

                    allCategories.clear();
                    allCategories.addAll(categories);

                    runOnUiThread(() -> {
                        List<Object> categoryObjects = new ArrayList<>(categories);

                        CategoryAdapter adapter = new CategoryAdapter(MainActivity.this, categoryObjects, category -> onMainCategoryClicked((Category) category), false);
                        recyclerViewTop.setAdapter(adapter);
                    });

                    // Fetch all products after fetching categories
                    fetchAllProducts();
                }
            }
        });
    }

    private void fetchAllProducts() {
        OkHttpClient client = new OkHttpClient();
        String token = getToken();
        Request request = new Request.Builder()
                .url("http://172.207.27.24:8100/v2/products")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "網路請求失敗", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "網路請求失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d(TAG, "API響應: " + responseBody);
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API錯誤: " + responseBody, Toast.LENGTH_LONG).show());
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray categoriesArray = jsonObject.getJSONArray("data");

                        for (int i = 0; i < categoriesArray.length(); i++) {
                            JSONObject categoryJson = categoriesArray.getJSONObject(i);
                            int categoryId = categoryJson.getInt("category_id");

                            List<Product> products = new ArrayList<>();
                            if (categoryJson.has("products")) {
                                JSONArray productsArray = categoryJson.getJSONArray("products");
                                for (int j = 0; j < productsArray.length(); j++) {
                                    JSONObject productJson = productsArray.getJSONObject(j);
                                    Integer quantity = productJson.isNull("quantity") ? null : productJson.getInt("quantity"); // Null check for quantity

                                    Product product = new Product(
                                            productJson.getInt("product_id"),
                                            productJson.getString("name"),
                                            productJson.optInt("price", 0), // Use optInt to handle null price
                                            quantity,  // 正確處理 quantity
                                            new ArrayList<>() // Addons can be processed similarly if needed
                                    );
                                    products.add(product);
                                }
                            }

                            for (Category category : allCategories) {
                                if (category.getCategory_id() == categoryId) {
                                    category.setProducts(products); // 将产品列表设置到类别
                                    break;
                                }
                            }
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "解析產品數據失敗", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "解析產品數據失敗", Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    private void onMainCategoryClicked(Category category) {
        // 清空副類別的 RecyclerView 內容
        runOnUiThread(() -> {
            CategoryAdapter emptyAdapter = new CategoryAdapter(MainActivity.this, new ArrayList<>(), null, false);
            recyclerViewBottom.setAdapter(emptyAdapter);
        });

        if (!category.getSub_categories().isEmpty()) {
            List<Object> subCategoryObjects = new ArrayList<>(category.getSub_categories());
            CategoryAdapter adapter = new CategoryAdapter(this, subCategoryObjects, subCategory -> onSubCategoryClicked((SubCategory) subCategory), false);
            recyclerViewBottom.setAdapter(adapter);
        } else {
            displayProductsForCategory(category.getCategory_id());
        }
    }

    private void onSubCategoryClicked(SubCategory subCategory) {
        displayProductsForCategory(subCategory.getCategory_id());
        fetchProducts(subCategory);
    }


    private void fetchProducts(SubCategory subCategory) {
        OkHttpClient client = new OkHttpClient();
        String token = getToken();
        Log.d(TAG, "使用的Token: " + token);
        Request request = new Request.Builder()
                .url("http://172.207.27.24:8100/v2/products")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "網路請求失敗", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "網路請求失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d(TAG, "API響應: " + responseBody);
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API錯誤: " + responseBody, Toast.LENGTH_LONG).show());
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        JSONArray categoriesArray = jsonObject.getJSONArray("data");

                        for (int i = 0; i < categoriesArray.length(); i++) {
                            JSONObject categoryJson = categoriesArray.getJSONObject(i);
                            JSONArray subCategoriesArray = categoryJson.getJSONArray("sub_categories");

                            for (int j = 0; j < subCategoriesArray.length(); j++) {
                                JSONObject subCategoryJson = subCategoriesArray.getJSONObject(j);
                                if (subCategoryJson.getInt("category_id") == subCategory.getCategory_id()) {
                                    List<Product> products = new ArrayList<>();
                                    JSONArray productsArray = subCategoryJson.getJSONArray("products");
                                    for (int k = 0; k < productsArray.length(); k++) {
                                        JSONObject productJson = productsArray.getJSONObject(k);

                                        // 解析 Addons
                                        JSONArray addonsArray = productJson.getJSONArray("addons");
                                        List<Addon> addons = new ArrayList<>();
                                        for (int l = 0; l < addonsArray.length(); l++) {
                                            JSONObject addonJson = addonsArray.getJSONObject(l);
                                            Addon addon = new Addon(
                                                    addonJson.getInt("addon_id"),
                                                    addonJson.getString("name"),
                                                    addonJson.getInt("price"),
                                                    addonJson.getInt("addon_category_id"),
                                                    addonJson.getString("addon_category_name")
                                            );
                                            addons.add(addon);
                                        }

                                        // 創建 Product 對象
                                        Product product = new Product(
                                                productJson.getInt("product_id"),
                                                productJson.getString("name"),
                                                productJson.getInt("price"),
                                                productJson.getInt("quantity"),
                                                addons
                                        );
                                        products.add(product);
                                    }
                                    subCategory.setProducts(products);
                                    break;
                                }
                            }
                        }

                        runOnUiThread(() -> displayProductsForCategory(subCategory.getCategory_id()));
                    } catch (JSONException e) {
                        Log.e(TAG, "解析產品數據失敗", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "解析產品數據失敗", Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    private void displayProductsForCategory(int categoryId) {
        List<Product> products = new ArrayList<>();
        for (Category cat : allCategories) {
            if (cat.getCategory_id() == categoryId) {
                List<Product> catProducts = cat.getProducts();
                if (catProducts != null) {
                    products.addAll(catProducts);
                }
                for (SubCategory subCat : cat.getSub_categories()) {
                    List<Product> subCatProducts = subCat.getProducts();
                    if (subCatProducts != null) {
                        products.addAll(subCatProducts);
                    }
                }
                break;
            }
            for (SubCategory subCat : cat.getSub_categories()) {
                if (subCat.getCategory_id() == categoryId) {
                    List<Product> subCatProducts = subCat.getProducts();
                    if (subCatProducts != null) {
                        products.addAll(subCatProducts);
                    }
                    break;
                }
            }
        }

        if (products.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "此類別下沒有產品", Toast.LENGTH_LONG).show());
        } else {
            runOnUiThread(() -> {
                List<Object> productObjects = new ArrayList<>(products);
                CategoryAdapter adapter = new CategoryAdapter(MainActivity.this, productObjects, product -> showProductDialog((Product) product), true);
                recyclerViewButtons.setAdapter(adapter);
            });
        }
    }







    private String getToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("jwt_token", "");
    }

    private void goToNextStep() {
        try {
            Log.d(TAG, "Navigating to CartActivity");
            Intent intent = new Intent(MainActivity.this, CartActivity.class);
            intent.putExtra("cartItems", new ArrayList<>(cartItems));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to CartActivity", e);
            Toast.makeText(this, "Error navigating to CartActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void goToMainActivity() {
        // 已經在MainActivity，不需要再跳轉
    }

    private void goToTransactionRecordsActivity() {
        Intent intent = new Intent(MainActivity.this, TransactionRecordsActivity.class);
        startActivity(intent);
    }

    private void goToCheckoutActivity() {
        Intent intent = new Intent(MainActivity.this, CheckoutActivity.class);
        startActivity(intent);
    }

    private void showProductDialog(Product product) {
        // 加载自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        TextView tvButtonName = dialogView.findViewById(R.id.tvButtonName);
        TextView tvStock = dialogView.findViewById(R.id.tvStock); // 新增庫存顯示
        EditText editTextQuantity = dialogView.findViewById(R.id.editTextQuantity);
        Button btnDecrease = dialogView.findViewById(R.id.btnDecrease);
        Button btnIncrease = dialogView.findViewById(R.id.btnIncrease);
        Button btnAddToCart = dialogView.findViewById(R.id.btnAddToCart);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        LinearLayout addonsLayout = dialogView.findViewById(R.id.addonsLayout);

        tvButtonName.setText(product.getProductName());  // 设置显示按钮名称
        tvStock.setText("庫存：" + product.getQuantity());  // 设定庫存顯示

        btnDecrease.setOnClickListener(v -> {
            int quantity = Integer.parseInt(editTextQuantity.getText().toString());
            if (quantity > 1) {
                quantity--;
                editTextQuantity.setText(String.valueOf(quantity));
            }
        });

        btnIncrease.setOnClickListener(v -> {
            int quantity = Integer.parseInt(editTextQuantity.getText().toString());
            quantity++;
            editTextQuantity.setText(String.valueOf(quantity));
        });

        // 动态生成addon按钮并设置点击事件
        List<Addon> selectedAddons = new ArrayList<>();
        for (Addon addon : product.getAddons()) {
            // Create a TextView for the addon category name
            TextView addonCategory = new TextView(this);
            addonCategory.setText(addon.getAddonCategoryName());
            addonsLayout.addView(addonCategory);

            // Create a Button for the addon
            Button addonButton = new Button(this);
            addonButton.setText(addon.getName());
            addonButton.setBackgroundColor(Color.LTGRAY); // Initial background color
            addonButton.setOnClickListener(v -> {
                if (selectedAddons.contains(addon)) {
                    selectedAddons.remove(addon);
                    addonButton.setBackgroundColor(Color.LTGRAY); // Reset background color
                } else {
                    selectedAddons.add(addon);
                    addonButton.setBackgroundColor(Color.GREEN); // Change background color to indicate selection
                }
            });
            addonsLayout.addView(addonButton);
        }

        // Create the dialog instance
        AlertDialog dialog = builder.create();

        btnAddToCart.setOnClickListener(v -> {
            try {
                int quantity = Integer.parseInt(editTextQuantity.getText().toString());
                CartItem cartItem = new CartItem(product.getProductId(), product.getProductName(), product.getPrice(), quantity, true);
                cartItem.setAddons(selectedAddons);

                // 不需要每次都重新加载购物车数据，因为这会覆盖当前的购物车状态
                // loadCartItemsFromPreferences(); // 加载最新的购物车数据

                cartItems.add(cartItem); // 添加新商品
                saveCartItemsToPreferences(); // 保存更新后的购物车数据到 SharedPreferences
                Log.d(TAG, "After adding item, cart items: " + cartItems.toString());

                if (cartAdapter != null) {
                    cartAdapter.notifyDataSetChanged();  // 更新适配器显示
                }

                Log.d("MainActivity", "Item added to cart: " + product.getProductName() + " with quantity: " + quantity);
                updateOrderSummary(quantity); // 更新订单摘要（如果有这个方法的话）
                dialog.dismiss();
            } catch (Exception e) {
                Log.e("MainActivity", "Error adding to cart", e);
                Toast.makeText(this, "Error adding to cart: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadCartItemsFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");
        Log.d("MainActivity", "Loaded cart items from SharedPreferences: " + cartItemsJson);

        Type listType = new TypeToken<ArrayList<CartItem>>() {}.getType();
        List<CartItem> loadedCartItems = new Gson().fromJson(cartItemsJson, listType);

        if (loadedCartItems != null) {
            cartItems.clear();
            cartItems.addAll(loadedCartItems);
        } else {
            cartItems.clear();
        }

        if (cartAdapter != null) {
            cartAdapter.notifyDataSetChanged(); // 通知适配器数据变化
            Log.d("MainActivity", "購物車適配器已通知數據變更");
        }
    }




    public void resetOrderCount() {
        orderCount = 0;
        updateOrderSummary(0); // 更新 UI 显示
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Starting to refresh and load cart items.");

        Intent intent = getIntent();
        if (intent.getBooleanExtra("fromCheckout", false)) {
            Log.d(TAG, "onResume: Detected return from checkout, clearing cart items.");
            clearCartItems();

            // 重置標誌，避免後續操作再次清空購物車
            intent.putExtra("fromCheckout", false);
            setIntent(intent); // 更新 intent
        } else {
            refreshCartItems();
        }

        Log.d(TAG, "onResume: Cart items refreshed and loaded.");
        updateOrderSummary(cartItems.size()); // 更新订单摘要
    }
    private void clearCartItems() {
        // 清空購物車數據
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("cartItems", "[]"); // 清空购物车数据
        editor.apply(); // 使用 apply 确保立即写入

        // 清空内存中的购物车列表
        if (cartItems != null) {
            cartItems.clear();
        }

        // 保存清空的購物車狀態
        saveCartItemsToPreferences();

        if (cartAdapter != null) {
            cartAdapter.notifyDataSetChanged(); // 通知适配器数据变化
        }

        // 顯示購物車已清空的提示
        Toast.makeText(this, "購物車已清空", Toast.LENGTH_LONG).show();
    }


    private void checkCartItems() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");

        if ("[]".equals(cartItemsJson)) {
            Toast.makeText(this, "購物車已清空", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "購物車未清空", Toast.LENGTH_LONG).show();
        }
    }

    private void refreshCartItems() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        String cartItemsJson = sharedPreferences.getString("cartItems", "[]");
        Log.d(TAG, "refreshCartItems: Loaded cart items from SharedPreferences: " + cartItemsJson);

        // 解析JSON並更新購物車列表
        Type listType = new TypeToken<ArrayList<CartItem>>() {}.getType();
        List<CartItem> loadedCartItems = new Gson().fromJson(cartItemsJson, listType);

        if (loadedCartItems != null) {
            cartItems.clear();
            cartItems.addAll(loadedCartItems);
        } else {
            cartItems.clear();
        }

        // 通知適配器數據變更
        if (cartAdapter != null) {
            cartAdapter.notifyDataSetChanged();
            Log.d(TAG, "refreshCartItems: 購物車適配器已通知數據變更");
        }

        // 顯示購物車是否已清空的提示
        if (!"[]".equals(cartItemsJson)) {
            Toast.makeText(this, "購物車未清空，含有數據！", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "購物車已清空", Toast.LENGTH_LONG).show();
        }
    }




    public void updateOrderSummary(int count) {
        textViewOrderSummary.setText("您已訂購的商品");
        layoutOrderSummary.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("jwt_token");
        editor.putBoolean("is_logged_in", false);
        editor.apply();

        // 返回登录界面
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}

