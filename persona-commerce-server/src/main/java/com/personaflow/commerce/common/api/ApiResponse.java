package com.personaflow.commerce.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.personaflow.commerce.common.error.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        int code,
        String message,
        String errorCode,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", null, data);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.defaultMessage());
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(1, message, errorCode.code(), null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(1, message, errorCode.code(), data);
    }
}
