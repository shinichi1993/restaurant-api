package com.restaurant.api.repository;

import com.restaurant.api.entity.Role;
import com.restaurant.api.entity.RolePermission;
import com.restaurant.api.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * RolePermissionRepository
 * -------------------------------------------------------------------
 * Repository thao tác với bảng role_permission (bảng trung gian).
 *
 * Cho phép:
 *  - Lấy danh sách permission theo role
 *  - Xóa tất cả permission của 1 role, v.v.
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * Tìm tất cả RolePermission theo 1 role.
     * Dùng để load toàn bộ quyền của role đó.
     */
    List<RolePermission> findByRole(Role role);

    /**
     * Tìm bản ghi RolePermission cụ thể theo role & permission.
     * Dùng cho check tồn tại trước khi thêm / xóa.
     */
    Optional<RolePermission> findByRoleAndPermission(Role role, Permission permission);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
