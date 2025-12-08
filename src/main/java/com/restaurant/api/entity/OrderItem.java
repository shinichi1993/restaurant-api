package com.restaurant.api.entity;

import com.restaurant.api.enums.OrderItemStatus;
import com.restaurant.api.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity OrderItem – Chi tiết từng món trong 1 order
 * -------------------------------------------------------------------
 * Phase 2 POS Advanced – nâng cấp để phục vụ:
 *   - Kitchen Display (màn hình bếp)
 *   - Trạng thái chế biến của TỪNG MÓN
 *   - Snapshot giá tại thời điểm order (không bị thay đổi theo Dish.price)
 *   - Ghi chú món (VD: ít cay, không hành...)
 *   - Quan hệ chuẩn: OrderItem → Order (ManyToOne) và Dish (ManyToOne)
 *
 * Các trường mới bổ sung:
 *   - snapshotPrice: giá món tại thời điểm gọi
 *   - status: NEW / SENT_TO_KITCHEN / COOKING / DONE / CANCELED
 *   - note: ghi chú riêng cho từng món
 * -------------------------------------------------------------------
 * Lưu ý:
 *   - Không dùng orderId/dishId kiểu Long nữa → chuyển sang quan hệ entity
 *   - Các service phải cập nhật theo entity mới để tránh lỗi
 */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    // --------------------------------------------------------------------
    // KHÓA CHÍNH
    // --------------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --------------------------------------------------------------------
    // QUAN HỆ VỚI ORDER (NHIỀU MÓN → 1 ĐƠN HÀNG)
    // --------------------------------------------------------------------
    /**
     * Order mà món này thuộc về.
     * -------------------------------------------------------
     * - Map với cột order_id trong bảng order_item
     * - fetch = LAZY để tránh load toàn order khi không cần
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // --------------------------------------------------------------------
    // QUAN HỆ VỚI DISH (1 MÓN ĂN)
    // --------------------------------------------------------------------
    /**
     * Món ăn được gọi.
     * -------------------------------------------------------
     * - Map với cột dish_id trong DB
     * - snapshotPrice sẽ copy từ dish.price tại thời điểm order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    // --------------------------------------------------------------------
    // SỐ LƯỢNG
    // --------------------------------------------------------------------
    @Column(nullable = false)
    private Integer quantity;

    // --------------------------------------------------------------------
    // SNAPSHOT GIÁ MÓN TẠI THỜI ĐIỂM ORDER
    // --------------------------------------------------------------------
    /**
     * snapshotPrice:
     * -------------------------------------------------------
     * - Giá của món tại thời điểm order.
     * - Dùng để tính tiền *không phụ thuộc* vào việc thay đổi giá sau này.
     * - Ví dụ: hôm nay Phở = 50,000 → ngày mai tăng 55,000
     *   nhưng order hôm qua vẫn phải tính 50,000.
     */
    @Column(name = "snapshot_price", nullable = false)
    private BigDecimal snapshotPrice;

    // --------------------------------------------------------------------
    // TRẠNG THÁI XỬ LÝ CỦA MÓN
    // --------------------------------------------------------------------
    /**
     * status:
     * -------------------------------------------------------
     * - Trạng thái chế biến của từng món.
     * - Hỗ trợ KitchenPage hiển thị tiến trình.
     *
     * Các trạng thái:
     *   NEW
     *   SENT_TO_KITCHEN
     *   COOKING
     *   DONE
     *   CANCELED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private OrderItemStatus status;

    // --------------------------------------------------------------------
    // GHI CHÚ CỦA MÓN
    // --------------------------------------------------------------------
    /**
     * Ghi chú riêng cho món:
     * -------------------------------------------------------
     * VD:
     *  - "ít cay"
     *  - "không hành"
     *  - "ưu tiên làm trước"
     */
    @Column(columnDefinition = "TEXT")
    private String note;

    // --------------------------------------------------------------------
    // THỜI GIAN TẠO / CẬP NHẬT
    // --------------------------------------------------------------------
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --------------------------------------------------------------------
    // LIFECYCLE HOOKS – TỰ SET THỜI GIAN
    // --------------------------------------------------------------------
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
