package com.restaurant.api.controller;

import com.restaurant.api.dto.user.*;
import com.restaurant.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

/**
 * UserController – Quản lý người dùng trong hệ thống.
 * Module 03 sẽ sử dụng controller này rất nhiều.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * API lấy danh sách user
     * - Trả về danh sách dạng UserResponse (không trả password)
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    /**
     * API lấy user theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    /**
     * API tạo user mới
     * - Dùng ở Module 03
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        return ResponseEntity.ok(userService.create(req));
    }

    /**
     * API cập nhật user theo ID
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest req
    ) {
        return ResponseEntity.ok(userService.update(id, req));
    }

    /**
     * API xóa user
     * - Là xóa mềm (chuyển status = INACTIVE)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok("Xóa thành công");
    }

    /**
     * API lấy thông tin user hiện tại (dựa vào token)
     * - Principal chứa username của người đang đăng nhập
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(userService.getByUsername(principal.getName()));
    }

    /**
     * API đổi mật khẩu
     * - Người dùng tự đổi mật khẩu của mình
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        userService.changePassword(principal.getName(), req);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
