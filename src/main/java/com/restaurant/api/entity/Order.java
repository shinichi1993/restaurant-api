package com.restaurant.api.entity;

import com.restaurant.api.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.restaurant.api.entity.RestaurantTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

/**
 * Entity Order – Lưu thông tin đơn gọi món của khách
 * ---------------------------------------------------------------
 * - Tương ứng bảng "orders" trong Flyway V9
 * - Mỗi order có nhiều orderItem
 * - Trừ kho / hoàn kho sẽ dựa trên orderItem + RecipeItem
 * ---------------------------------------------------------------
 * Các trường chính:
 * - orderCode: mã order hiển thị (ORD20250101001)
 * - totalPrice: tổng tiền
 * - status: NEW / SERVING / PAID / CANCELED
 * - createdBy: user nào tạo order
 * - createdAt / updatedAt: thời gian
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode; // Mã đơn

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice; // Tổng tiền của đơn

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // Trạng thái đơn (NEW / SERVING / PAID / CANCELED)

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú đơn hàng

    // Người tạo đơn (tham chiếu app_user.id)
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * ID hội viên gắn với order (nếu có).
     * ------------------------------------------------------------
     * - Dùng để tính điểm Loyalty cho hóa đơn này.
     * - Không dùng @ManyToOne để tránh join nặng.
     * - FE sẽ gửi memberId khi chọn khách hàng trên màn Order.
     */
    @Column(name = "member_id")
    private Long memberId;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Thời gian tạo

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Thời gian cập nhật

    /**
     * Bàn mà đơn hàng này đang phục vụ.
     * ----------------------------------------------------
     * - Liên kết với bảng restaurant_table qua cột table_id
     * - Được set khi tạo order từ màn hình chọn bàn (Module 16)
     */
    @ManyToOne
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    /**
     * Tự set thời gian tạo khi persist
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Tự cập nhật thời gian sửa cuối
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
