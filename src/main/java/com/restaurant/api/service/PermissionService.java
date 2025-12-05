package com.restaurant.api.service;

import com.restaurant.api.dto.role.PermissionResponse;
import com.restaurant.api.entity.Permission;
import com.restaurant.api.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PermissionService
 * =====================================================================
 * Service phụ trách các nghiệp vụ liên quan đến PERMISSION:
 *
 *  - Lấy danh sách toàn bộ quyền trong hệ thống
 *  - (Tùy chọn) sau này có thể thêm CREATE / UPDATE / DELETE permission
 *
 * Hiện tại phục vụ chủ yếu cho:
 *  - Màn hình gán quyền cho Role (Module 13)
 * =====================================================================
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    /**
     * Lấy tất cả quyền trong hệ thống.
     * Dùng cho:
     *  - Form tạo / sửa Role
     *  - Màn hình xem danh sách quyền
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();

        return permissions.stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .description(p.getDescription())
                        .build()
                )
                .collect(Collectors.toList());
    }
}
