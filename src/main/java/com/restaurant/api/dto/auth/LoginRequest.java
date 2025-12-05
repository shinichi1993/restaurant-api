package com.restaurant.api.dto.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * LoginRequest – Dữ liệu gửi lên khi đăng nhập
 */
@Getter
@Setter
public class LoginRequest {
    private String username; // Tên đăng nhập
    private String password; // Mật khẩu
}
