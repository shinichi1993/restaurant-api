package com.restaurant.api.dto.order;

import com.restaurant.api.enums.OrderItemStatus;
import lombok.*;
import java.math.BigDecimal;

/**
 * OrderItemResponse
 * ------------------------------------------------------------
 * DTO trả về thông tin chi tiết TỪNG MÓN trong order.
 * Phase 2 POS Advanced bổ sung:
 *   - Trạng thái chế biến (status)
 *   - Giá snapshot tại thời điểm order (dishPrice)
 *   - Ghi chú món (note)
 *
 * Dùng cho các màn hình:
 *   - OrderDetailModal
 *   - PaymentPage
 *   - POS OrderPage
 *   - KitchenPage (quan trọng)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long dishId;            // ID món
    private String dishName;        // Tên món

    /**
     * Giá món tại thời điểm order.
     * ----------------------------------------------------
     * - Được lấy từ OrderItem.snapshotPrice
     * - Không phụ thuộc giá hiện tại của Dish
     */
    private BigDecimal dishPrice;

    private Integer quantity;       // Số lượng

    /**
     * Tổng tiền = snapshotPrice × quantity
     */
    private BigDecimal subtotal;

    /**
     * Trạng thái chế biến hiện tại của món.
     * ----------------------------------------------------
     * NEW → SENT_TO_KITCHEN → COOKING → DONE → (CANCELED)
     */
    private OrderItemStatus status;

    /**
     * Ghi chú riêng của món.
     */
    private String note;
}
