package com.restaurant.api.repository;

import com.restaurant.api.entity.User;
import com.restaurant.api.entity.UserRole;
import com.restaurant.api.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * UserRoleRepository
 * -------------------------------------------------------------------
 * Repository thao tác với bảng user_role (bảng trung gian user <-> role).
 *
 * Một số hàm thường dùng:
 *  - findByUser(...) để lấy danh sách role của 1 user
 *  - findByUserAndRole(...) để kiểm tra user đã có role đó chưa
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Lấy toàn bộ UserRole của 1 user.
     * Dùng để biết user hiện tại đang có những role nào.
     */
    List<UserRole> findByUser(User user);

    /**
     * Tìm 1 bản ghi UserRole theo user + role.
     * Dùng cho việc tránh gán trùng hoặc khi xóa gán role.
     */
    Optional<UserRole> findByUserAndRole(User user, Role role);

    /**
     * Lấy danh sách permission code của user theo username.
     *
     * Luồng JOIN:
     *   UserRole
     *    → Role
     *    → RolePermission
     *    → Permission
     *
     * Trả về List<String> để:
     *  - Tránh LazyInitializationException
     *  - Dùng trực tiếp cho Spring Security (authorities)
     */
    @Query("""
        SELECT DISTINCT p.code
        FROM UserRole ur
        JOIN ur.role r
        JOIN RolePermission rp ON rp.role.id = r.id
        JOIN Permission p ON rp.permission.id = p.id
        WHERE ur.user.username = :username
    """)
    List<String> findPermissionCodesByUsername(@Param("username") String username);

    /**
     * Lấy danh sách role.code theo userId
     * - Trả về String để tránh Lazy
     */
    @Query("""
        SELECT r.code
        FROM UserRole ur
        JOIN ur.role r
        WHERE ur.user.id = :userId
    """)
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * Xóa toàn bộ role của user theo userId
     * - Dùng khi update roles: clear hết rồi insert lại
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
