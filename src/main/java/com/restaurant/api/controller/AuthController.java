package com.restaurant.api.controller;

import com.restaurant.api.dto.auth.*;
import com.restaurant.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * AuthController – Xử lý các API liên quan đến đăng nhập, đăng ký,
 * refresh token và đăng xuất trong hệ thống.
 *
 * Toàn bộ comment theo Rule 13: phải dùng tiếng Việt.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * API đăng nhập hệ thống
     * - Nhận username + password
     * - Trả về accessToken + refreshToken nếu hợp lệ
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * API đăng ký tài khoản
     * - Chỉ dùng trong module 01 (admin tạo tài khoản gốc)
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("Đăng ký thành công");
    }

    /**
     * API refresh token
     * - FE sẽ gọi khi accessToken hết hạn
     * - Trả về accessToken mới
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * Đăng xuất hệ thống (RESTful chuẩn)
     * - Xóa refresh token khỏi DB
     * - username truyền trong URL
     */
    @Transactional
    @DeleteMapping("/logout/{username}")
    public ResponseEntity<String> logout(@PathVariable String username) {
        authService.logout(username);
        return ResponseEntity.ok("Đăng xuất thành công");
    }

}
