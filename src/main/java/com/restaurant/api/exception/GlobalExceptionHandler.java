package com.restaurant.api.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler
 * -------------------------------------------------------------
 * Bắt toàn bộ lỗi RuntimeException và trả về JSON đúng format để FE đọc được.
 * Nếu không có file này, Spring Boot sẽ trả HTML error page → FE không parse được
 * → Axios hiểu sai thành lỗi 401.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt lỗi RuntimeException
     * -------------------------------------------------------------
     * Ví dụ:
     * throw new RuntimeException("Hệ thống chưa bật chế độ POS đơn giản");
     * → FE sẽ nhận:
     * { "message": "Hệ thống chưa bật chế độ POS đơn giản" }
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body); // 400 Bad Request
    }

    /**
     * Bắt các lỗi không mong muốn khác
     * -------------------------------------------------------------
     * Đảm bảo luôn trả về JSON.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Lỗi hệ thống: " + ex.getMessage());
        return ResponseEntity.internalServerError().body(body); // 500
    }
}
