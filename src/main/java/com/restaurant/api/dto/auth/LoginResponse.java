package com.restaurant.api.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
}
