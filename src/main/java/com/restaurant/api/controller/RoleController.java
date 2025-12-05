package com.restaurant.api.controller;

import com.restaurant.api.dto.role.RoleDetailResponse;
import com.restaurant.api.dto.role.RoleRequest;
import com.restaurant.api.dto.role.RoleResponse;
import com.restaurant.api.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RoleController – Quản lý vai trò (Role)
 * =====================================================================
 * Cung cấp các API REST cho màn hình:
 *  - Danh sách role
 *  - Xem chi tiết role (kèm danh sách quyền)
 *  - Tạo mới role
 *  - Cập nhật role
 *  - Xóa role
 *
 * Đường dẫn chung: /api/roles
 *
 * Lưu ý:
 *  - Các API này sẽ được bảo vệ bởi SecurityConfig (yêu cầu đăng nhập).
 *  - Phân quyền cấp sâu hơn (chỉ ADMIN mới gọi được) có thể bổ sung sau.
 * =====================================================================
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // =================================================================
    // 1. LẤY DANH SÁCH ROLE
    // =================================================================

    /**
     * API: Lấy toàn bộ danh sách role trong hệ thống.
     * -------------------------------------------------------------
     * Method: GET
     * URL   : /api/roles
     *
     * Dùng cho:
     *  - Màn hình quản lý vai trò (table roles)
     *  - Dropdown chọn role khi gán cho user
     *
     * Response body:
     *  [
     *    {
     *      "id": 1,
     *      "name": "Quản trị hệ thống",
     *      "code": "ADMIN",
     *      "description": "Toàn quyền hệ thống"
     *    },
     *    ...
     *  ]
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> result = roleService.getAllRoles();
        return ResponseEntity.ok(result);
    }

    // =================================================================
    // 2. LẤY CHI TIẾT 1 ROLE (KÈM DANH SÁCH QUYỀN)
    // =================================================================

    /**
     * API: Lấy chi tiết 1 role theo id.
     * -------------------------------------------------------------
     * Method: GET
     * URL   : /api/roles/{id}
     *
     * Dùng cho:
     *  - Màn hình xem chi tiết role
     *  - Form update role (load sẵn danh sách quyền đang gán)
     *
     * Response body mẫu:
     * {
     *   "id": 1,
     *   "name": "Quản trị hệ thống",
     *   "code": "ADMIN",
     *   "description": "Toàn quyền",
     *   "permissions": [
     *      { "id": 1, "code": "USER_VIEW", "name": "Xem người dùng", ... },
     *      ...
     *   ]
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleDetailResponse> getRoleDetail(@PathVariable Long id) {
        RoleDetailResponse result = roleService.getRoleDetail(id);
        return ResponseEntity.ok(result);
    }

    // =================================================================
    // 3. TẠO MỚI ROLE
    // =================================================================

    /**
     * API: Tạo mới 1 role.
     * -------------------------------------------------------------
     * Method: POST
     * URL   : /api/roles
     *
     * Request body (RoleRequest):
     * {
     *   "name": "Nhân viên thu ngân",
     *   "code": "CASHIER",
     *   "description": "Chỉ được thanh toán, không sửa menu",
     *   "permissionIds": [ 3, 5, 7 ]
     * }
     *
     * Quy trình:
     *  - Validate đơn giản name/code không rỗng (làm ở Service).
     *  - Lưu Role.
     *  - Gán các Permission theo permissionIds.
     *  - Trả về RoleDetailResponse (để reload form).
     */
    @PostMapping
    public ResponseEntity<RoleDetailResponse> createRole(@RequestBody RoleRequest request) {
        RoleDetailResponse result = roleService.createRole(request);
        return ResponseEntity.ok(result);
    }

    // =================================================================
    // 4. CẬP NHẬT ROLE
    // =================================================================

    /**
     * API: Cập nhật thông tin role + danh sách quyền.
     * -------------------------------------------------------------
     * Method: PUT
     * URL   : /api/roles/{id}
     *
     * Request body (RoleRequest) giống tạo mới:
     * {
     *   "name": "Nhân viên thu ngân (sửa)",
     *   "code": "CASHIER",
     *   "description": "Chỉ được thanh toán, xem order",
     *   "permissionIds": [ 3, 5, 8 ]
     * }
     *
     * Quy trình trong service:
     *  - B1: Lấy role hiện tại.
     *  - B2: Update name/code/description.
     *  - B3: Xóa hết RolePermission cũ của role.
     *  - B4: Tạo lại RolePermission từ permissionIds mới.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleDetailResponse> updateRole(
            @PathVariable Long id,
            @RequestBody RoleRequest request
    ) {
        RoleDetailResponse result = roleService.updateRole(id, request);
        return ResponseEntity.ok(result);
    }

    // =================================================================
    // 5. XÓA ROLE
    // =================================================================

    /**
     * API: Xóa 1 role theo id.
     * -------------------------------------------------------------
     * Method: DELETE
     * URL   : /api/roles/{id}
     *
     * Quy tắc (do Service xử lý):
     *  - KHÔNG cho xóa nếu role đang được gán cho bất kỳ user nào
     *    (user_role vẫn còn bản ghi với role này).
     *  - Nếu không còn user dùng:
     *      + Xóa toàn bộ RolePermission của role.
     *      + Xóa role.
     *
     * Response:
     *  - 200 OK + message đơn giản (hoặc body rỗng).
     *  - Nếu đang được dùng → throw RuntimeException với message tiếng Việt.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok().build();
    }
}
