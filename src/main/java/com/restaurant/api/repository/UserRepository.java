package com.restaurant.api.repository;

import com.restaurant.api.entity.User;
import com.restaurant.api.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * UserRepository – Truy vấn dữ liệu bảng app_user
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm theo username để login
    Optional<User> findByUsername(String username);

    // Kiểm tra username tồn tại
    boolean existsByUsername(String username);

    // Lấy danh sách user theo status
    List<User> findByStatus(UserStatus status);

    // UserRepository – Truy vấn dữ liệu bảng app_user
    // -----------------------------------------------------------
    // - Nếu cần lọc user theo role → join user_role + role.code
    // -----------------------------------------------------------

    @Query("""
    SELECT u
    FROM User u
    JOIN UserRole ur ON ur.user.id = u.id
    JOIN Role r ON ur.role.id = r.id
    WHERE r.code = :roleCode
    """)
    List<User> findByRoleCode(@Param("roleCode") String roleCode);
}
