package com.restaurant.api.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * UserCreateRequest – Dữ liệu tạo user mới
 */
@Getter
@Setter
public class UserCreateRequest {
    private String username;
    private String password;
    private String fullName;
    private String phone;
    private String role;   // ADMIN hoặc STAFF
}
