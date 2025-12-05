package com.restaurant.api.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * OrderCreateRequest
 * ------------------------------------------------------------
 * Request tạo order
 * Gồm:
 *  - tableId: ID bàn mà khách đang ngồi (Module 16)
 *  - items: danh sách món được gọi
 *  - note: ghi chú thêm (tùy chọn)
 * ------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

    /**
     * ID bàn mà khách đang ngồi.
     * ------------------------------------------------------------
     * Module 16 bổ sung:
     *  - FE phải gửi tableId khi tạo order
     *  - BE sẽ đánh dấu bàn thành OCCUPIED
     *  - Nếu tableId = null → order không gán vào bàn nào
     */
    private Long tableId;

    @NotEmpty(message = "Order phải có ít nhất 1 món")
    @Valid
    private List<OrderItemRequest> items; // Danh sách món gọi

    private String note; // Ghi chú tùy chọn
}
