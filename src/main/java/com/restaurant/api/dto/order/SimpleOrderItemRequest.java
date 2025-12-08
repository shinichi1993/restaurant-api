package com.restaurant.api.dto.order;

import lombok.Getter;
import lombok.Setter;

/**
 * SimpleOrderItemRequest
 * ------------------------------------------------------------------------
 * DTO đại diện cho TỪNG MÓN khi tạo đơn trong Simple POS Mode.
 *
 * Dùng cho API:
 *   - POST /api/orders/simple-create
 *
 * Các field:
 *   - dishId   : ID của món ăn
 *   - quantity : số lượng món
 *   - note     : ghi chú món (tùy chọn, có thể null)
 *
 * Lưu ý:
 *   - Không chứa giá tiền → BE sẽ tự lấy Dish.price để set snapshotPrice.
 *   - Không dùng cho flow POS truyền thống / Kitchen Mode.
 */
@Getter
@Setter
public class SimpleOrderItemRequest {

    /**
     * ID của món ăn (Dish.id)
     * ----------------------------------------------------
     * BE sẽ dùng để load entity Dish tương ứng.
     */
    private Long dishId;

    /**
     * Số lượng món được gọi.
     * ----------------------------------------------------
     * Bắt buộc > 0, validation chi tiết có thể bổ sung sau.
     */
    private Integer quantity;

    /**
     * Ghi chú riêng cho món (VD: ít cay, không hành...)
     * ----------------------------------------------------
     * Có thể để null/empty nếu không có yêu cầu đặc biệt.
     */
    private String note;
}
