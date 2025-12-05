package com.restaurant.api.dto.order;

import lombok.*;
import java.math.BigDecimal;

/**
 * OrderItemResponse
 * ------------------------------------------------------------
 * DTO trả về chi tiết từng món trong order
 * Dùng cho:
 *  - API xem chi tiết order
 *  - FE OrderDetailModal / PaymentPage
 * ------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long dishId;       // ID món

    private String dishName;   // Tên món
    private BigDecimal dishPrice; // Giá món tại thời điểm gọi

    private Integer quantity;  // Số lượng

    private BigDecimal subtotal; // dishPrice × quantity
}
