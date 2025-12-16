package com.restaurant.api.service;

import com.restaurant.api.dto.user.*;
import com.restaurant.api.entity.User;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.UserStatus;
import com.restaurant.api.repository.UserRepository;
import com.restaurant.api.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService
 * ----------------------------------------------------------
 * Xử lý toàn bộ nghiệp vụ liên quan đến người dùng:
 *  - Lấy danh sách user
 *  - Lấy chi tiết user theo ID
 *  - Lấy user theo username (/me)
 *  - Tạo user mới
 *  - Cập nhật user
 *  - Xóa user (xóa mềm)
 *  - Đổi mật khẩu cá nhân
 *  - Chuyển đổi Entity → DTO
 * ----------------------------------------------------------
 * Toàn bộ comment tiếng Việt theo Rule 13.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UserRoleRepository userRoleRepository;

    /**
     * Lấy toàn bộ người dùng trong hệ thống.
     * Không trả về mật khẩu — chỉ trả DTO UserResponse.
     */
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy người dùng theo ID.
     * Nếu không tìm thấy → ném lỗi.
     */
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return toUserResponse(user);
    }

    /**
     * Lấy user theo username — dùng cho API /me
     */
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return toUserResponse(user);
    }

    /**
     * Tạo người dùng mới.
     * - Kiểm tra username đã tồn tại chưa
     * - Mã hóa mật khẩu
     * - Set role + status (Enum)
     */
    public UserResponse create(UserCreateRequest req) {

        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
        // ✅ GHI AUDIT LOG – tạo user mới
        auditLogService.log(
                AuditAction.USER_CREATE,
                "user",                // tên entity
                user.getId(),          // entityId
                null,                  // beforeData (chưa cần)
                user                   // afterData – tình trạng sau khi tạo
        );
        return toUserResponse(user);
    }

    /**
     * Cập nhật thông tin user (ADMIN dùng)
     * - Đổi họ tên
     * - Đổi vai trò (Enum)
     * - Đổi trạng thái (Enum)
     */
    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setFullName(req.getFullName());

        // Chuyển đổi status từ request → Enum
        if (req.getStatus() != null) {
            user.setStatus(UserStatus.valueOf(req.getStatus()));
        }

        userRepository.save(user);

        // ✅ GHI AUDIT LOG – update user
        auditLogService.log(
                AuditAction.USER_UPDATE,
                "user",                // tên entity
                user.getId(),          // entityId
                null,                  // beforeData (chưa cần)
                user                   // afterData – tình trạng sau khi tạo
        );

        return toUserResponse(user);
    }

    /**
     * Xóa người dùng (xóa mềm bằng cách đổi trạng thái)
     */
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        // ✅ GHI AUDIT LOG – delete user
        auditLogService.log(
                AuditAction.USER_DELETE,
                "user",                // tên entity
                user.getId(),          // entityId
                null,                  // beforeData (chưa cần)
                user                   // afterData – tình trạng sau khi tạo
        );
    }

    /**
     * Đổi mật khẩu cá nhân (tự người dùng đổi)
     */
    public void changePassword(String username, ChangePasswordRequest req) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Ánh xạ Entity User → DTO UserResponse
     * Dùng ở tất cả API trả dữ liệu người dùng.
     */
    public UserResponse toUserResponse(User user) {
        // 🔹 Lấy danh sách role code của user
        List<String> roleCodes = userRoleRepository.findByUser(user)
                .stream()
                .map(ur -> ur.getRole().getCode())
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(roleCodes) // ✅ ROLE THẬT
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Lấy UserID qua UserName
     */
    public Long getUserIdByUsername(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user từ token"));
        return u.getId();
    }


}
