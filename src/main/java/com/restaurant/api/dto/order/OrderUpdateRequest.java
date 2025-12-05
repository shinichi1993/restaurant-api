package com.restaurant.api.dto.order;

import lombok.*;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * OrderUpdateRequest
 * ------------------------------------------------------------
 * DTO đại diện cho request sửa lại danh sách món trong Order.
 * Dùng cho:
 *  - POS: khi nhân viên thêm / xoá món rồi bấm "Gửi Order"
 *  - Admin: sửa lại order nếu cần (tùy business)
 *
 * Quy tắc:
 *  - Order phải có ít nhất 1 món.
 *  - Mỗi món gồm dishId + quantity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OrderUpdateRequest {

    @NotEmpty(message = "Danh sách món không được để trống")
    private List<OrderItemRequest> items;  // Danh sách món mới
}
