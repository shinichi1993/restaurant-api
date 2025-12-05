package com.restaurant.api.repository;

import com.restaurant.api.entity.RefreshToken;
import com.restaurant.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository quản lý RefreshToken
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Tìm RefreshToken theo chuỗi token
    Optional<RefreshToken> findByToken(String token);

    // Tìm RefreshToken theo user
    Optional<RefreshToken> findByUser(User user);

    // Xóa RefreshToken theo user (dùng khi logout)
    void deleteByUser(User user);
}
