package com.restaurant.api.repository;

import com.restaurant.api.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * OrderItemRepository
 * ------------------------------------------------------------
 * Repository làm việc với bảng order_item
 * Chức năng:
 *  - Lấy danh sách món thuộc 1 order (order_id)
 *  - Xóa toàn bộ order_item theo order (dùng khi hủy order)
 * ------------------------------------------------------------
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Lấy danh sách OrderItem theo ID đơn hàng
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Xóa toàn bộ item của order
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM order_item WHERE order_id = :orderId", nativeQuery = true)
    void deleteByOrderId(@Param("orderId") Long orderId);
}
