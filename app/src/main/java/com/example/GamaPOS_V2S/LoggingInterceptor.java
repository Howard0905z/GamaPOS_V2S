package com.example.GamaPOS_V2S;
import android.util.Log;

import okhttp3.Interceptor;
import okhttp3.Response;
import java.io.IOException;

public class LoggingInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        // 这里你可以按照需要记录任何部分的响应数据
        Log.d("HTTP Response", "URL: " + response.request().url());
        Log.d("HTTP Response", "Response Code: " + response.code());
        Log.d("HTTP Response", "Response Body: " + response.peekBody(Long.MAX_VALUE).string()); // 注意，这个操作会消耗响应体的流
        return response;
    }
}