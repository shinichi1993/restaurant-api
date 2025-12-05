package com.restaurant.api.repository;

import com.restaurant.api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository Category – thao tác DB.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name); // Tìm theo tên (duy nhất)
}
