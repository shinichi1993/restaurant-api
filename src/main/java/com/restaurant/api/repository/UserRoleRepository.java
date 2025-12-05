package com.restaurant.api.repository;

import com.restaurant.api.entity.User;
import com.restaurant.api.entity.UserRole;
import com.restaurant.api.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
