package com.example.finmate.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
    private long timestamp;

    public ResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ResponseDTO(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO<>(true, "성공", data);
    }

    public static <T> ResponseDTO<T> success(String message, T data) {
        return new ResponseDTO<>(true, message, data);
    }

    public static <T> ResponseDTO<T> success(String message) {
        return new ResponseDTO<>(true, message);
    }

    public static <T> ResponseDTO<T> error(String message) {
        return new ResponseDTO<>(false, message);
    }

    public static <T> ResponseDTO<T> error(String message, T data) {
        return new ResponseDTO<>(false, message, data);
    }
}