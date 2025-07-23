package com.example.finmate.common.exception;

import com.example.finmate.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLockedException(
            AccountLockedException ex, WebRequest request) {
        log.warn("계정 잠금 예외: {}", ex.getMessage());
        return ResponseEntity.status(423) // 423 Locked
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("ACCOUNT_LOCKED")
                        .timestamp(System.currentTimeMillis())
                        .traceId(UUID.randomUUID().toString())
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        log.warn("인증 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요.")
                        .errorCode("AUTHENTICATION_FAILED")
                        .timestamp(System.currentTimeMillis())
                        .traceId(java.util.UUID.randomUUID().toString())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("유효성 검증 실패: {}", errors);

        return ResponseEntity.badRequest()
                .body(ApiResponse.builder()
                        .success(false)
                        .message("입력값이 유효하지 않습니다.")
                        .data(errors)
                        .errorCode("VALIDATION_ERROR")
                        .timestamp(System.currentTimeMillis())
                        .traceId(UUID.randomUUID().toString())
                        .build());
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("잘못된 요청: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("INVALID_REQUEST")
                        .timestamp(System.currentTimeMillis())
                        .traceId(java.util.UUID.randomUUID().toString())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("예상치 못한 오류 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("서버 오류가 발생했습니다.")
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .timestamp(System.currentTimeMillis())
                        .traceId(java.util.UUID.randomUUID().toString())
                        .build());
    }
}