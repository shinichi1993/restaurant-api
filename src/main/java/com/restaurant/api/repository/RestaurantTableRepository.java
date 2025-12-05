package com.restaurant.api.repository;

import com.restaurant.api.entity.RestaurantTable;
import com.restaurant.api.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác với bảng restaurant_table.
 */
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    /**
     * Tìm danh sách bàn theo trạng thái.
     */
    List<RestaurantTable> findByStatus(TableStatus status);

    /**
     * Kiểm tra tên bàn đã tồn tại chưa (phục vụ validate khi tạo/sửa).
     */
    boolean existsByName(String name);

    /**
     * Tìm bàn theo tên.
     */
    Optional<RestaurantTable> findByName(String name);
}
