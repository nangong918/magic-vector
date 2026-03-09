package com.openapi.component.handler;


import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.exception.AppException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理器
 * @author 13225
 * @date 2025/1/3 17:24
 * TODO 待测试
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // Service层的AppException异常抛给前端
    @ExceptionHandler(AppException.class)
    public ResponseEntity<BaseResponse<Object>> handleAppException(AppException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.LogBackError(ex.getErrCode(), ex.getMessage()));
    }

    // @RequestBody 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(it -> it.getDefaultMessage())
                .orElse(CommonExceptions.PARAM_ERROR.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR.getCode(), message));
    }

    // @RequestParam/@PathVariable 参数校验异常
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR.getCode(), ex.getMessage()));
    }

}
