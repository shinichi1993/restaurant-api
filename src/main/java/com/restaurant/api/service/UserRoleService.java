package com.restaurant.api.service;

import com.restaurant.api.dto.user.UpdateUserRolesRequest;
import com.restaurant.api.dto.user.UserRolesResponse;
import com.restaurant.api.entity.Role;
import com.restaurant.api.entity.User;
import com.restaurant.api.entity.UserRole;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.repository.RoleRepository;
import com.restaurant.api.repository.UserRepository;
import com.restaurant.api.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UserRoleService
 * ------------------------------------------------------------------
 * Service xử lý nghiệp vụ gán role cho user (Module 4.1.6).
 *
 * Chức năng:
 *  - Lấy danh sách roles của user
 *  - Cập nhật roles của user (clear rồi insert lại)
 *
 * Quy tắc an toàn:
 *  1) Không cho user tự remove role ADMIN của chính mình (tránh tự khóa)
 *  2) User phải có ít nhất 1 role (tối thiểu STAFF)
 * ------------------------------------------------------------------
 */
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditLogService auditLogService;

    /**
     * Lấy danh sách role codes của user theo userId.
     */
    @Transactional(readOnly = true)
    public UserRolesResponse getRolesOfUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<String> roleCodes = userRoleRepository.findRoleCodesByUserId(userId);

        return UserRolesResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roleCodes)
                .build();
    }

    /**
     * Cập nhật roles cho user.
     *
     * @param actorUsername username người thao tác (lấy từ Principal)
     * @param userId user bị cập nhật role
     * @param req danh sách role codes
     */
    @Transactional
    public UserRolesResponse updateRolesOfUser(String actorUsername, Long userId, UpdateUserRolesRequest req) {

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Role hiện tại (before) để audit
        List<String> beforeRoles = userRoleRepository.findRoleCodesByUserId(userId);

        // Chuẩn hóa input: trim + distinct + uppercase
        List<String> incoming = Optional.ofNullable(req.getRoles())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.toList());

        // Rule: phải có ít nhất 1 role
        if (incoming.isEmpty()) {
            throw new RuntimeException("User phải có ít nhất 1 vai trò");
        }

        /*
        // Rule: bắt buộc tối thiểu STAFF (nếu m muốn bắt buộc)
        // -> Nếu không muốn ép STAFF thì comment block này.
        if (!incoming.contains("STAFF") && !incoming.contains("ADMIN")) {
            // Tuỳ hệ thống: nếu có nhiều role khác thì bỏ điều kiện này.
            // Ở đây set rule tối thiểu có STAFF hoặc ADMIN cho dễ vận hành.
            throw new RuntimeException("User phải có tối thiểu STAFF hoặc ADMIN");
        }
         */

        // Rule: không cho tự remove ADMIN của chính mình
        if (actorUsername != null && actorUsername.equals(targetUser.getUsername())) {
            boolean beforeIsAdmin = beforeRoles.contains("ADMIN");
            boolean afterIsAdmin = incoming.contains("ADMIN");
            if (beforeIsAdmin && !afterIsAdmin) {
                throw new RuntimeException("Bạn không thể tự gỡ vai trò ADMIN của chính mình");
            }
        }

        // Validate: tất cả role code phải tồn tại trong bảng role
        List<Role> roles = roleRepository.findAll().stream().toList();
        Set<String> existed = roles.stream().map(Role::getCode).collect(Collectors.toSet());

        for (String code : incoming) {
            if (!existed.contains(code)) {
                throw new RuntimeException("Role không tồn tại: " + code);
            }
        }

        // Clear toàn bộ roles cũ
        userRoleRepository.deleteByUserId(userId);

        // Insert lại theo danh sách mới
        for (String code : incoming) {
            Role role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + code));

            UserRole ur = UserRole.builder()
                    .user(targetUser)
                    .role(role)
                    .build();

            userRoleRepository.save(ur);
        }

        // Audit log
        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("roles", beforeRoles);

        Map<String, Object> afterData = new HashMap<>();
        afterData.put("roles", incoming);

        auditLogService.log(
                AuditAction.USER_UPDATE, // hoặc tạo enum riêng USER_ROLE_UPDATE nếu m có
                "user_role",
                userId,
                beforeData,
                afterData
        );

        return UserRolesResponse.builder()
                .userId(targetUser.getId())
                .username(targetUser.getUsername())
                .roles(incoming)
                .build();
    }
}
