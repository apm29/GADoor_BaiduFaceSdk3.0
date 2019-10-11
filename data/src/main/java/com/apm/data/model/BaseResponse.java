package com.apm.data.model;

import java.util.ArrayList;
import java.util.List;

public class BaseResponse<T> {
    public String status;
    public String text;
    public String token;
    public T data;

    public boolean success() {
        return status != null && status.equals("1");
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "status='" + status + '\'' +
                ", text='" + text + '\'' +
                ", token='" + token + '\'' +
                ", data=" + data +
                '}';
    }

    public BaseResponse(String status, String text, String token, T data) {
        this.status = status;
        this.text = text;
        this.token = token;
        this.data = data;
    }

    public static <T> BaseResponse<List<T>> emptyList(){
        return new BaseResponse<List<T>>(
                "1","请求失败","",new ArrayList<T>()
        );
    }

    public static <T> BaseResponse<T> error(){
        return new BaseResponse<>(
                "0", "请求失败", "", null
        );
    }
}
