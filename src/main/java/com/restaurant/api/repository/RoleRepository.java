package com.restaurant.api.repository;

import com.restaurant.api.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * RoleRepository
 * ---------------------------------------------------------------
 * Repository thao tác với bảng role.
 *
 * Cung cấp các hàm:
 *  - findByCode(...) để lấy role theo mã (ADMIN, STAFF...)
 *  - Các hàm CRUD cơ bản thừa kế từ JpaRepository.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tìm role theo mã code.
     * VD: "ADMIN", "STAFF"...
     */
    Optional<Role> findByCode(String code);
}
