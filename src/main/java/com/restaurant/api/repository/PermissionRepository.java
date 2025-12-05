package com.restaurant.api.repository;

import com.restaurant.api.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * PermissionRepository
 * ---------------------------------------------------------------
 * Repository thao tác với bảng permission.
 *
 * Cung cấp hàm:
 *  - findByCode(...) để tìm permission theo mã code
 *  - Các hàm CRUD cơ bản kế thừa từ JpaRepository.
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Tìm permission theo mã code (ORDER_VIEW, ORDER_CREATE...).
     */
    Optional<Permission> findByCode(String code);
}
