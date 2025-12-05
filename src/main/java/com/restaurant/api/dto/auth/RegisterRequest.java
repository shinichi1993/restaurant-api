package com.restaurant.api.dto.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * RegisterRequest – Dữ liệu gửi lên để đăng ký tài khoản mới
 */
@Getter
@Setter
public class RegisterRequest {
    private String username;   // Tên đăng nhập
    private String password;   // Mật khẩu
    private String fullName;   // Họ tên
    private String phone;      // Số điện thoại
    private String role;       // Vai trò (ADMIN hoặc STAFF)
}
