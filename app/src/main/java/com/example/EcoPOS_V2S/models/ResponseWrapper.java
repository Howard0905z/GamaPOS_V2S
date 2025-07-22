package com.example.EcoPOS_V2S.models;

public class ResponseWrapper<T> {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
