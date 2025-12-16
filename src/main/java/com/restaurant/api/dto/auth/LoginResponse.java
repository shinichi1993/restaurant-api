package com.restaurant.api.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * LoginResponse – Kết quả trả về khi đăng nhập thành công
 */
@Getter
@Setter
@Builder
public class LoginResponse {
    private String accessToken;  // JWT Token truy cập
    private String refreshToken; // Refresh token
    private String tokenType;    // Loại token (Bearer)
    // ✅ Module 4.1: danh sách permission code của user
    private List<String> permissions;
}
