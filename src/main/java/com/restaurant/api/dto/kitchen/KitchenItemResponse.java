package com.restaurant.api.dto.kitchen;

import com.restaurant.api.enums.OrderItemStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KitchenItemResponse
 * ------------------------------------------------------------
 * DTO dùng cho màn hình BẾP (Kitchen Display).
 *
 * Mỗi record đại diện cho 1 món trong 1 order:
 *  - orderItemId : ID của bản ghi OrderItem
 *  - orderId     : ID đơn hàng
 *  - orderCode   : Mã đơn (dễ nhìn trên màn hình bếp)
 *  - tableId     : ID bàn (có thể null nếu order không gán bàn)
 *  - tableName   : Tên bàn (nếu có)
 *  - dishId      : ID món
 *  - dishName    : Tên món
 *  - quantity    : Số lượng cần chế biến
 *  - status      : Trạng thái món (NEW, SENT_TO_KITCHEN, COOKING, DONE, CANCELED)
 *  - note        : Ghi chú của món (nếu có)
 *  - createdAt   : Thời điểm tạo OrderItem (để sort trên màn hình bếp)
 */
@Data
@Builder
public class KitchenItemResponse {

    private Long orderItemId;
    private Long orderId;
    private String orderCode;

    private Long tableId;
    private String tableName;

    private Long dishId;
    private String dishName;
    private Integer quantity;

    private OrderItemStatus status;
    private String note;

    private LocalDateTime createdAt;
}
