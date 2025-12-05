package com.restaurant.api.dto.role;

import lombok.*;

import java.util.List;

/**
 * RoleDetailResponse
 * -------------------------------------------------------------------
 * DTO trả về CHI TIẾT 1 role, bao gồm:
 *  - Thông tin role (id, name, code, description)
 *  - Danh sách quyền (permissions) đã gán cho role.
 *
 * Dùng cho màn hình:
 *  - Xem chi tiết role
 *  - Form update role (load các quyền hiện tại)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDetailResponse {

    /** ID role. */
    private Long id;

    /** Tên role. */
    private String name;

    /** Mã role duy nhất. */
    private String code;

    /** Mô tả role. */
    private String description;

    /** Danh sách quyền đang thuộc role này. */
    private List<PermissionResponse> permissions;
}
