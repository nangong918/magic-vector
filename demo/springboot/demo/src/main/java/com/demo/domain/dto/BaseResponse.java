package com.demo.domain.dto;

import com.demo.domain.constant.ExceptionEnums;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;

@Slf4j
@Data
public class BaseResponse<T> implements Serializable {
    protected String code = "200";
    private String message;
    private T data;

    public BaseResponse(T data) {
        this.code = "200";
        this.message = null;
        this.data = data;
    }

    public BaseResponse(String message, T data) {
        this.code = "200";
        this.message = message;
        this.data = data;
    }

    public BaseResponse(String code, String message, T data) {
        this.code = code == null ? "200" : code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(String message) {
        this.code = "200";
        this.message = message;
        this.data = null;
    }

    public static <T> ResponseEntity<BaseResponse<T>> getResponseEntitySuccessRE(T data) {
        BaseResponse<T> baseResponse = new BaseResponse<>(data);
        return ResponseEntity.ok(baseResponse);
    }

    public static <T> BaseResponse<T> getResponseEntitySuccess(T data) {
        return new BaseResponse<>(data);
    }

    public static <T> ResponseEntity<BaseResponse<T>> getResponseEntityFail() {
        return ResponseEntity.notFound().build();
    }

    public static <T> BaseResponse<T> LogBackError(String warningMessage, org.slf4j.Logger log) {
        log.warn(warningMessage);
        return new BaseResponse<>("400", warningMessage, null);
    }

    public static <T> BaseResponse<T> LogBackError(String warningMessage) {
        return new BaseResponse<>("400", warningMessage, null);
    }

    public static <T> BaseResponse<T> LogBackError(String errorCode, String errorMessage) {
        return new BaseResponse<>(errorCode, errorMessage, null);
    }

    public static <T> BaseResponse<T> LogBackError(ExceptionEnums exceptionEnums) {
        return new BaseResponse<>(exceptionEnums.getCode(), exceptionEnums.getMessage(), null);
    }
}
