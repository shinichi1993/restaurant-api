package com.restaurant.api.repository;

import com.restaurant.api.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * IngredientRepository
 * ---------------------------------------
 * Repository thao tác với bảng ingredient.
 * Sử dụng JpaRepository cung cấp sẵn CRUD:
 *  - findAll()
 *  - findById()
 *  - save()
 *  - delete()
 * ---------------------------------------
 * Module 04 chưa cần custom query riêng.
 */
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
