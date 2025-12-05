package com.restaurant.api.dto.role;

import lombok.*;

/**
 * RoleResponse
 * -------------------------------------------------------------------
 * DTO trả về danh sách role đơn giản cho màn hình:
 *  - Danh sách vai trò (bảng Role)
 *  - Dropdown chọn role, v.v.
 *
 * Không bao gồm danh sách permission chi tiết.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    /** ID role (khóa chính). */
    private Long id;

    /** Tên role hiển thị. */
    private String name;

    /** Mã role duy nhất (code). */
    private String code;

    /** Mô tả ngắn. */
    private String description;
}
