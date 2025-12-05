package com.restaurant.api.dto.order;

import com.restaurant.api.enums.OrderStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderResponse
 * ------------------------------------------------------------
 * DTO trả thông tin đầy đủ của 1 order:
 *  - Tổng tiền
 *  - Trạng thái
 *  - Ghi chú
 *  - Người tạo
 *  - Ngày tạo / cập nhật
 *  - Danh sách món (OrderItemResponse)
 * ------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;              // ID order
    private String orderCode;     // Mã order

    private BigDecimal totalPrice; // Tổng tiền

    private OrderStatus status;   // Trạng thái

    private String note;          // Ghi chú

    private Long createdBy;       // Ai tạo đơn

    private LocalDateTime createdAt; // Ngày tạo
    private LocalDateTime updatedAt; // Ngày cập nhật

    private List<OrderItemResponse> items; // Danh sách món
}
