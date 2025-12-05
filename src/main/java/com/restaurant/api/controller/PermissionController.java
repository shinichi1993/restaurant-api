package com.restaurant.api.controller;

import com.restaurant.api.dto.role.PermissionResponse;
import com.restaurant.api.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PermissionController – Quản lý quyền (Permission)
 * =====================================================================
 * Controller đơn giản trả danh sách quyền trong hệ thống.
 *
 * Mục đích:
 *  - FE gọi để hiển thị danh sách quyền (checkbox, table...)
 *  - Dùng trong form tạo / sửa Role (Module 13).
 *
 * Đường dẫn: /api/permissions
 * =====================================================================
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // =================================================================
    // 1. DANH SÁCH PERMISSION
    // =================================================================

    /**
     * API: Lấy toàn bộ danh sách quyền.
     * -------------------------------------------------------------
     * Method: GET
     * URL   : /api/permissions
     *
     * Dùng cho:
     *  - Màn hình cấu hình phân quyền
     *  - Form gán quyền cho Role
     *
     * Response body:
     *  [
     *    {
     *      "id": 1,
     *      "code": "USER_VIEW",
     *      "name": "Xem người dùng",
     *      "description": "Cho phép xem danh sách user"
     *    },
     *    ...
     *  ]
     */
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> result = permissionService.getAllPermissions();
        return ResponseEntity.ok(result);
    }

    // Sau này nếu cần có thể bổ sung:
    //  - Tạo mới permission
    //  - Cập nhật / xóa permission
    // Hiện tại chỉ cần danh sách là đủ cho Module 13.
}
