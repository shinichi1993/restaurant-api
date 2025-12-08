package com.restaurant.api.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * OrderItemRequest
 * ------------------------------------------------------------
 * DTO cho từng món trong request tạo mới hoặc cập nhật order.
 * Phase 2 POS Advanced bổ sung:
 *   - Thêm trường note (ghi chú cho món)
 *   - Dữ liệu này sẽ được copy sang OrderItem.note
 *
 * Các trường:
 *  - dishId   : ID món ăn được gọi
 *  - quantity : số lượng
 *  - note     : ghi chú riêng (ít cay, không hành...)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    /**
     * ID món ăn được chọn trong order.
     * ----------------------------------------------------
     * - Không được để null
     */
    @NotNull(message = "Món ăn không được để trống")
    private Long dishId;

    /**
     * Số lượng món được gọi.
     * ----------------------------------------------------
     * - Bắt buộc >= 1
     */
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity;

    /**
     * Ghi chú riêng cho từng món (tùy chọn).
     * ----------------------------------------------------
     * Ví dụ:
     *  - "ít cay"
     *  - "không hành"
     *  - "nhiều đá"
     *  - "làm trước"
     *
     * Nếu FE không gửi → BE set null.
     */
    private String note;
}
