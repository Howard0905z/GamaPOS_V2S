package com.example.EcoPOS_V2S.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.EcoPOS_V2S.R;

public class LinePayActivity extends Activity {
    private static final String TAG = "LinePayActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linepay); // 使用新的布局文件

        // 获取布局中的控件
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView textViewStatus = findViewById(R.id.textViewStatus);

        // 获取Intent中的数据
        Intent intent = getIntent();
        if (intent != null) {
            // 处理支付逻辑，例如展示支付界面
            Log.d(TAG, "LinePayActivity started with intent: " + intent);
            // 模拟支付完成并返回结果
            textViewStatus.setText("Payment completed successfully!"); // 更新状态信息
            progressBar.setVisibility(ProgressBar.GONE); // 隐藏进度条

            // 模拟支付完成后的返回结果
            Intent resultIntent = new Intent();
            resultIntent.putExtra("status", "SUCCESS");
            resultIntent.putExtra("message", "Payment was successful");
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Failed to start LinePayActivity: no intent data", Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }
}