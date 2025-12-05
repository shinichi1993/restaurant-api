package com.restaurant.api.dto.role;

import lombok.*;

/**
 * PermissionResponse
 * -------------------------------------------------------------------
 * DTO trả về thông tin 1 quyền (permission) cho FE.
 *
 * Dùng cho:
 *  - Màn hình cấu hình quyền
 *  - Popup gán quyền cho role
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    /** ID quyền. */
    private Long id;

    /** Mã quyền (VD: "ORDER_VIEW", "ORDER_UPDATE"). */
    private String code;

    /** Tên hiển thị cho người dùng. */
    private String name;

    /** Mô tả chi tiết hơn (optional). */
    private String description;
}
