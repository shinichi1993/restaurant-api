package com.restaurant.api.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * UserUpdateRequest – Dữ liệu cập nhật user
 */
@Getter
@Setter
public class UserUpdateRequest {
    private String fullName;
    private String phone;
    private String role;    // ADMIN hoặc STAFF
    private String status;  // ACTIVE hoặc INACTIVE
}
