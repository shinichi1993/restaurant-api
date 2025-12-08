package com.restaurant.api.repository;

import com.restaurant.api.entity.OrderItem;
import com.restaurant.api.enums.OrderItemStatus;
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
     * Lấy danh sách OrderItem theo ID đơn hàng.
     * ----------------------------------------------------
     * Sử dụng property path "order.id" thay vì orderId.
     */
    List<OrderItem> findByOrder_Id(Long orderId);

    // =====================================================================
    // 2) LẤY LIST ITEM THEO DANH SÁCH TRẠNG THÁI
    // =====================================================================

    /**
     * Lấy danh sách item theo nhiều trạng thái cùng lúc.
     *
     * Dùng trong:
     *  - KitchenService.getKitchenItems()
     *  - Dashboard muốn xem số món đang COOKING
     */
    List<OrderItem> findByStatusIn(List<OrderItemStatus> statuses);


    // =====================================================================
    // 3) QUERY TỐI ƯU CHO MÀN HÌNH BẾP
    // =====================================================================

    /**
     * Query đặc thù cho KitchenPage:
     *  - Lấy tất cả món có trạng thái thuộc NEW / SENT_TO_KITCHEN / COOKING
     *  - Order không được là PAID hoặc CANCELED
     *  - Trả về item mới nhất trước
     *
     * Lưu ý:
     *  - Dùng JOIN FETCH để load Order và Dish trong 1 query → tránh N+1.
     *  - Order.status lọc trực tiếp trong query để giảm tải BE.
     */
    @Query("""
        SELECT oi
        FROM OrderItem oi
        JOIN FETCH oi.order o
        JOIN FETCH oi.dish d
        WHERE oi.status IN :statuses
          AND o.status NOT IN ('PAID', 'CANCELED')
        ORDER BY oi.createdAt ASC
    """)
    List<OrderItem> findKitchenItems(
            @Param("statuses") List<OrderItemStatus> statuses
    );


    // =====================================================================
    // 4) UPDATE TRẠNG THÁI MÓN KHÔNG CẦN LOAD ENTITY
    // =====================================================================

    /**
     * Update trạng thái item theo ID.
     * Không cần load entity → nhanh hơn, nhẹ hơn.
     *
     * Dùng cho:
     *  - Kitchen update status
     *  - POS muốn chuyển nhiều item nhanh
     */
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.status = :newStatus WHERE oi.id = :id")
    void updateStatusById(
            @Param("id") Long id,
            @Param("newStatus") OrderItemStatus newStatus
    );


    // =====================================================================
    // 5) ĐẾM SỐ LƯỢNG ITEM TRONG ORDER
    // =====================================================================

    /**
     * Đếm số item thuộc 1 order.
     * Dùng để kiểm tra xem order còn món nào chưa hoàn thành hay không.
     */
    long countByOrder_Id(Long orderId);


    // =====================================================================
    // 6) LẤY ITEM CỦA ORDER THEO STATUS
    // =====================================================================

    /**
     * Lấy item theo orderId + status.
     * Dùng khi:
     *  - Muốn kiểm tra xem món nào đang COOKING
     *  - Auto chuyển order sang SERVING / FINISHED
     */
    List<OrderItem> findByOrder_IdAndStatus(Long orderId, OrderItemStatus status);

    /**
     * Xóa toàn bộ item của order
     */
    @Modifying
    @Transactional
    void deleteByOrder_Id(@Param("orderId") Long orderId);
}
