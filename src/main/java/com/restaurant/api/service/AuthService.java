package com.restaurant.api.service;

import com.restaurant.api.dto.auth.*;
import com.restaurant.api.entity.RefreshToken;
import com.restaurant.api.entity.Role;
import com.restaurant.api.entity.User;
import com.restaurant.api.entity.UserRole;
import com.restaurant.api.enums.UserStatus;
import com.restaurant.api.repository.RefreshTokenRepository;
import com.restaurant.api.repository.RoleRepository;
import com.restaurant.api.repository.UserRepository;
import com.restaurant.api.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuthService
 * ----------------------------------------------------------
 * Xử lý nghiệp vụ xác thực:
 *  - Đăng nhập
 *  - Đăng ký tài khoản
 *  - Làm mới access token (refresh token)
 *  - Đăng xuất
 *  - Lưu refresh token cho user
 * ----------------------------------------------------------
 * Toàn bộ comment tiếng Việt theo Rule 13.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PermissionQueryService permissionQueryService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Đăng nhập hệ thống:
     * - Xác thực username/password
     * - Kiểm tra trạng thái tài khoản
     * - Sinh accessToken + refreshToken
     * - Lưu refreshToken vào DB
     */
    public LoginResponse login(LoginRequest req) {

        // Xác thực tài khoản bằng Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        // Lấy thông tin user
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Nếu tài khoản bị khóa → cấm đăng nhập
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("Tài khoản đang bị khóa");
        }

        // Sinh token
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Lưu refresh token vào DB
        saveUserRefreshToken(user, refreshToken);

        List<String> permissions = permissionQueryService.getPermissionCodesByUsername(user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .permissions(permissions)
                .build();
    }

    /**
     * Đăng ký tài khoản mới.
     * - Kiểm tra username trùng
     * - Mã hoá mật khẩu
     * - Set role & status dạng Enum
     */
    public void register(RegisterRequest req) {

        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        // 1. Tạo user (KHÔNG set role)
        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        // 2. Gán role mặc định qua bảng user_role (Option A)
        String roleCode = (req.getRole() != null && !req.getRole().isBlank())
                ? req.getRole()
                : "STAFF";

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleCode));

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();

        userRoleRepository.save(userRole);
    }

    /**
     * Làm mới Access Token khi hết hạn.
     * - Xác thực refresh token hợp lệ
     * - Kiểm tra thời gian hết hạn
     * - Sinh Access Token mới
     */
    public RefreshTokenResponse refreshToken(RefreshTokenRequest req) {

        String token = req.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        // Kiểm tra refresh token đã hết hạn chưa
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token đã hết hạn");
        }

        // Sinh Access Token mới từ user
        String newAccessToken = jwtService.generateAccessToken(refreshToken.getUser());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    /**
     * Đăng xuất:
     * - Xoá refresh token của user khỏi DB
     * - FE chỉ cần gửi username trong URL
     */
    public void logout(String username) {

        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            refreshTokenRepository.deleteByUser(user);
        }
    }

    /**
     * Lưu refresh token vào DB:
     * - Nếu user đã có refresh token → update
     * - Nếu chưa có → tạo mới
     */
    private void saveUserRefreshToken(User user, String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7)); // Hạn refresh: 7 ngày

        refreshTokenRepository.save(refreshToken);
    }
}
