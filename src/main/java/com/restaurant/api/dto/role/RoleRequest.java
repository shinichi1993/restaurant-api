package com.restaurant.api.dto.role;

import lombok.*;

import java.util.Set;

/**
 * RoleRequest
 * -------------------------------------------------------------------
 * DTO dùng cho các API CREATE / UPDATE Role.
 *
 * FE sẽ gửi lên:
 *  - name          : Tên role (VD: "Quản trị hệ thống")
 *  - code          : Mã role duy nhất (VD: "ADMIN", "STAFF")
 *  - description   : Mô tả ngắn
 *  - permissionIds : Tập các ID quyền (permission) gán cho role
 *
 * Lưu ý:
 *  - permissionIds có thể null hoặc rỗng → không gán quyền nào.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {

    /** Tên hiển thị của role. */
    private String name;

    /** Mã role duy nhất, dùng cho logic phân quyền (VD: ADMIN, CASHIER...). */
    private String code;

    /** Mô tả ngắn về role. */
    private String description;

    /** Danh sách ID permission được gán cho role này. */
    private Set<Long> permissionIds;
}
