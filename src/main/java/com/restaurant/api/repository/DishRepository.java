package com.restaurant.api.repository;

import com.restaurant.api.entity.Dish;
import com.restaurant.api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Dish – thao tác DB cho bảng món ăn.
 * -----------------------------------------------------
 * - Tìm theo category
 * - Kiểm tra trùng tên trong cùng category
 * -----------------------------------------------------
 */
public interface DishRepository extends JpaRepository<Dish, Long> {

    // Lấy danh sách món theo Category (dùng cho FE filter và Recipe module)
    List<Dish> findByCategory(Category category);

    // Kiểm tra trùng tên trong cùng một Category
    Optional<Dish> findByNameAndCategory(String name, Category category);

}
