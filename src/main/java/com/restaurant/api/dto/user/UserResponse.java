package com.restaurant.api.dto.user;

import com.restaurant.api.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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
    private UserStatus status;         // Trạng thái
    // ✅ DANH SÁCH ROLE CỦA USER (ADMIN, STAFF...)
    private List<String> roles;
    private LocalDateTime createdAt;   // Ngày tạo
    private LocalDateTime updatedAt;   // Ngày cập nhật
}
