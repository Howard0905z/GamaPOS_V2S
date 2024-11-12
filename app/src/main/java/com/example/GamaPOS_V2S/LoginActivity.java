package com.example.GamaPOS_V2S;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        adjustFontScale(getResources().getConfiguration());
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login(editTextUsername.getText().toString().trim(), editTextPassword.getText().toString().trim());
            }
        });
    }

    private boolean isLoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("is_logged_in", false);
    }


    private void clearCart() {
        SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("cartItems", "[]"); // 清空购物车内容
        editor.apply();
    }

    public void adjustFontScale(Configuration configuration) {
        if (configuration.fontScale != 1) {
            configuration.fontScale = (float) 0.85; // 设置为你需要的比例因子
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.fontScale * metrics.density;
            getBaseContext().getResources().updateConfiguration(configuration, metrics);
        }
    }

    private void login(String username, String password) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url("http://172.207.27.24:8100/login") // 确保这是正确的登录 URL
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("LoginActivity", "网络请求失败", e);
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "网络请求失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String jsonData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);
                        final String token = jsonObject.optString("access_token", "default_value");
                        Log.d("LoginActivity", "Fetched token: " + token);

                        if ("default_value".equals(token)) {
                            Log.e("LoginActivity", "Token not found in the response");
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Token not found in the response", Toast.LENGTH_LONG).show());
                        } else {
                            saveToken(token);
                            runOnUiThread(() -> {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("LoginActivity", "Failed to parse token", e);
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error parsing login response", Toast.LENGTH_LONG).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "登录失败，请检查用户名和密码", Toast.LENGTH_LONG).show());
                }
            }
            private void saveToken(String token) {
                SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("jwt_token", token);
                editor.putBoolean("is_logged_in", true);  // 添加登录状态标志
                editor.apply();
            }


        });
    }

}
