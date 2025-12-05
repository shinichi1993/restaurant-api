package com.restaurant.api.dto.user;

import com.restaurant.api.enums.UserRole;
import com.restaurant.api.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * UserResponse – Trả về thông tin người dùng cho FE
 */
@Getter
@Setter
@Builder
public class UserResponse {

    private Long id;                   // ID người dùng
    private String username;           // Tên đăng nhập
    private String fullName;           // Họ tên
    private String phone;              // Số điện thoại
    private UserRole role;             // Vai trò
    private UserStatus status;         // Trạng thái
    private LocalDateTime createdAt;   // Ngày tạo
    private LocalDateTime updatedAt;   // Ngày cập nhật
}
