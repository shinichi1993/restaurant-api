package com.restaurant.api.repository;

import com.restaurant.api.entity.User;
import com.restaurant.api.enums.UserRole;
import com.restaurant.api.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // Lấy danh sách user theo role
    List<User> findByRole(UserRole role);

    // Lấy danh sách user theo status
    List<User> findByStatus(UserStatus status);
}
