package com.restaurant.api.repository;

import com.restaurant.api.entity.Order;
import com.restaurant.api.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import com.restaurant.api.enums.OrderStatus;
import java.util.List;
import java.util.Optional;

/**
 * OrderRepository
 * ------------------------------------------------------------
 * Repository làm việc với bảng orders
 * Các chức năng chính:
 *  - Lấy tất cả đơn có trạng thái chỉ định
 *  - Lọc đơn theo khoảng ngày
 *  - Lọc đơn theo cả trạng thái + khoảng ngày
 * ------------------------------------------------------------
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Tìm danh sách đơn theo trạng thái (NEW, SERVING, PAID…)
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Lọc danh sách order theo khoảng thời gian tạo
     */
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Lọc theo trạng thái + khoảng thời gian
     */
    List<Order> findByStatusAndCreatedAtBetween(
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Tìm 1 order đang mở theo bàn (NEW hoặc SERVING).
     * Dùng cho các nghiệp vụ: gộp bàn, chuyển bàn...
     */
    Optional<Order> findFirstByTableIdAndStatusIn(Long tableId, List<OrderStatus> statuses);

    /**
     * Tìm order đang mở theo bàn.
     * ---------------------------------------------------------
     * - tableId: id của bàn
     * - status: trạng thái order cần tìm (ví dụ: CREATED / IN_PROGRESS...)
     *
     * Lưu ý:
     *  - Chỉ trả về 1 order (Optional)
     *  - Nếu 1 bàn chỉ được phép có 1 order đang mở → dùng hàm này là đủ
     */
    Optional<Order> findByTableIdAndStatus(Long tableId, OrderStatus status);
}
