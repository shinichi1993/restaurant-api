package com.restaurant.api.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * UpdateUserRolesRequest
 * ------------------------------------------------------------------
 * Request cập nhật danh sách role cho 1 user.
 *
 * Ví dụ payload:
 * {
 *   "roles": ["ADMIN", "STAFF"]
 * }
 *
 * Quy ước:
 * - roles là list role.code (VD: ADMIN/STAFF)
 * ------------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRolesRequest {

    /**
     * Danh sách role codes cần gán cho user
     * - Bắt buộc phải có ít nhất 1 role
     */
    @NotEmpty(message = "Danh sách vai trò không được để trống")
    private List<String> roles;
}
