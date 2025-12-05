package com.restaurant.api.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * ChangePasswordRequest – Yêu cầu đổi mật khẩu
 */
@Getter
@Setter
public class ChangePasswordRequest {
    private String oldPassword; // Mật khẩu cũ
    private String newPassword; // Mật khẩu mới
}
