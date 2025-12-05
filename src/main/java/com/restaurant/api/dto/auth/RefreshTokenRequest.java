package com.restaurant.api.dto.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * RefreshTokenRequest – Nhận refresh token từ FE gửi lên để tạo access token mới
 */
@Getter
@Setter
public class RefreshTokenRequest {
    private String refreshToken;
}
