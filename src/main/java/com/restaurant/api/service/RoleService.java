package com.restaurant.api.service;

import com.restaurant.api.dto.role.PermissionResponse;
import com.restaurant.api.dto.role.RoleDetailResponse;
import com.restaurant.api.dto.role.RoleRequest;
import com.restaurant.api.dto.role.RoleResponse;
import com.restaurant.api.entity.Permission;
import com.restaurant.api.entity.Role;
import com.restaurant.api.entity.RolePermission;
import com.restaurant.api.entity.UserRole;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.repository.PermissionRepository;
import com.restaurant.api.repository.RolePermissionRepository;
import com.restaurant.api.repository.RoleRepository;
import com.restaurant.api.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RoleService
 * =====================================================================
 * Service xử lý toàn bộ nghiệp vụ liên quan đến ROLE:
 *
 *  - Lấy danh sách Role (list chung)
 *  - Lấy chi tiết 1 Role (kèm danh sách Permission)
 *  - Tạo mới Role + gán Permission
 *  - Cập nhật Role + Permission
 *  - Xóa Role (có kiểm tra ràng buộc UserRole)
 *
 * Lưu ý:
 *  - Không xử lý logic login / JWT ở đây.
 *  - Chỉ tập trung CRUD và mapping Role <-> Permission.
 *  - Toàn bộ comment tiếng Việt (Rule 13).
 * =====================================================================
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditLogService auditLogService;

    // =================================================================
    // 1. DANH SÁCH ROLE
    // =================================================================

    /**
     * Lấy toàn bộ danh sách role trong hệ thống.
     * Dùng cho:
     *  - Màn hình quản lý vai trò
     *  - Dropdown chọn role (gán cho user)
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();

        return roles.stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    // =================================================================
    // 2. CHI TIẾT 1 ROLE + DANH SÁCH PERMISSION
    // =================================================================

    /**
     * Lấy chi tiết 1 role, bao gồm:
     *  - Thông tin Role
     *  - Các Permission đang được gán cho Role
     *
     * @param roleId ID role
     * @return RoleDetailResponse
     */
    @Transactional(readOnly = true)
    public RoleDetailResponse getRoleDetail(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò"));

        // Lấy tất cả RolePermission rồi lọc theo role
        List<RolePermission> allRolePerms = rolePermissionRepository.findAll();

        List<RolePermission> rolePerms = allRolePerms.stream()
                .filter(rp -> rp.getRole() != null && Objects.equals(rp.getRole().getId(), roleId))
                .toList();

        // Map sang PermissionResponse
        List<PermissionResponse> permissions = rolePerms.stream()
                .map(RolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());

        return RoleDetailResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }

    // =================================================================
    // 3. TẠO ROLE MỚI
    // =================================================================

    /**
     * Tạo mới 1 role và gán danh sách permission kèm theo.
     *
     * Quy trình:
     *  - B1: Validate input cơ bản (tên, mã role không rỗng)
     *  - B2: Tạo entity Role, lưu DB
     *  - B3: Nếu có permissionIds → tạo danh sách RolePermission
     *  - B4: Trả về RoleDetailResponse cho FE
     */
    @Transactional
    public RoleDetailResponse createRole(RoleRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên role không được để trống");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new RuntimeException("Mã role không được để trống");
        }

        // Tạo mới role
        Role role = Role.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim())
                .description(request.getDescription())
                .build();

        roleRepository.save(role);

        // Gán quyền nếu có
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {

            List<Permission> permissions =
                    permissionRepository.findAllById(request.getPermissionIds());

            List<RolePermission> rolePerms = new ArrayList<>();
            for (Permission p : permissions) {
                RolePermission rp = RolePermission.builder()
                        .role(role)
                        .permission(p)
                        .build();
                rolePerms.add(rp);
            }

            rolePermissionRepository.saveAll(rolePerms);
        }

        // ✅ Audit log tạo role
        auditLogService.log(
                AuditAction.ROLE_CREATE,
                "role",
                role.getId(),
                null,
                role
        );

        // Trả về chi tiết
        return getRoleDetail(role.getId());
    }

    // =================================================================
    // 4. CẬP NHẬT ROLE
    // =================================================================

    /**
     * Cập nhật thông tin role + danh sách permission.
     *
     * Quy trình:
     *  - B1: Lấy role từ DB
     *  - B2: Update các field name/code/description
     *  - B3: Xóa toàn bộ RolePermission cũ của role
     *  - B4: Tạo lại RolePermission từ danh sách permissionIds mới
     */
    @Transactional
    public RoleDetailResponse updateRole(Long roleId, RoleRequest request) {

        // B1: Lấy role
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò"));

        // B2: Cập nhật thông tin cơ bản
        role.setName(request.getName().trim());
        role.setCode(request.getCode().trim());
        role.setDescription(request.getDescription());
        roleRepository.save(role);

        // ===============================
        // B3: XÓA QUYỀN CŨ CỦA ROLE
        // ===============================
        rolePermissionRepository.deleteByRoleId(roleId);

        // ===============================
        // B4: TẠO LẠI QUYỀN MỚI
        // ===============================
        Set<Long> permIds = Optional.ofNullable(request.getPermissionIds())
                .orElse(Collections.emptySet());

        if (!permIds.isEmpty()) {
            List<Permission> perms = permissionRepository.findAllById(permIds);

            List<RolePermission> newList = new ArrayList<>();
            for (Permission p : perms) {
                newList.add(
                        RolePermission.builder()
                                .role(role)
                                .permission(p)
                                .build()
                );
            }

            rolePermissionRepository.saveAll(newList);
        }

        // ✅ Audit log cập nhật role
        auditLogService.log(
                AuditAction.ROLE_UPDATE,
                "role",
                role.getId(),
                null,
                role
        );

        // B5: trả response chuẩn
        return getRoleDetail(role.getId());
    }

    // =================================================================
    // 5. XÓA ROLE
    // =================================================================

    /**
     * Xóa role.
     *
     * Quy tắc an toàn:
     *  - Không cho xóa nếu role đang được gán cho bất kỳ User nào
     *    (bảng user_role vẫn còn bản ghi với role này).
     *  - Nếu không còn user nào dùng:
     *      + Xóa tất cả RolePermission của role
     *      + Xóa role
     */
    @Transactional
    public void deleteRole(Long roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò"));

        // Kiểm tra còn ai dùng role này không
        List<UserRole> allUserRoles = userRoleRepository.findAll();
        boolean inUse = allUserRoles.stream()
                .anyMatch(ur -> ur.getRole() != null && Objects.equals(ur.getRole().getId(), roleId));

        if (inUse) {
            throw new RuntimeException("Không thể xóa role vì đang được gán cho người dùng");
        }

        // Xóa RolePermission của role
        List<RolePermission> allRolePerms = rolePermissionRepository.findAll();
        List<RolePermission> toDelete = allRolePerms.stream()
                .filter(rp -> rp.getRole() != null && Objects.equals(rp.getRole().getId(), roleId))
                .toList();

        if (!toDelete.isEmpty()) {
            rolePermissionRepository.deleteAll(toDelete);
        }

        // Xóa role
        roleRepository.delete(role);

        // ✅ Audit log xóa role
        auditLogService.log(
                AuditAction.ROLE_DELETE,
                "role",
                role.getId(),
                role,
                null
        );
    }

    // =================================================================
    // 6. HÀM CONVERT ENTITY → DTO
    // =================================================================

    /** Convert Role → RoleResponse (dùng cho list). */
    private RoleResponse toRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .build();
    }

    /** Convert Permission → PermissionResponse. */
    private PermissionResponse toPermissionResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .build();
    }
}
