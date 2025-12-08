package com.restaurant.api.dto.order;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * SimpleOrderRequest
 * ------------------------------------------------------------------------
 * DTO dùng cho API tạo đơn nhanh trong Simple POS Mode.
 *
 * Dùng cho API:
 *   - POST /api/orders/simple-create
 *
 * Các field:
 *   - tableId : ID bàn (có thể null nếu simple_pos_require_table = false)
 *   - items   : danh sách món được gọi (SimpleOrderItemRequest)
 *
 * Lưu ý:
 *   - Không chứa thông tin giá / discount / VAT.
 *   - Toàn bộ phần tính tiền, snapshotPrice sẽ do BE xử lý.
 */
@Getter
@Setter
public class SimpleOrderRequest {

    /**
     * ID bàn (RestaurantTable.id)
     * ----------------------------------------------------
     * - Nếu setting simple_pos_require_table = true:
     *     → BẮT BUỘC phải có tableId.
     * - Nếu setting simple_pos_require_table = false:
     *     → Cho phép null, order sẽ coi như đơn mang đi (takeaway).
     */
    private Long tableId;

    /**
     * Danh sách món được gọi trong đơn.
     * ----------------------------------------------------
     * - Mỗi phần tử là 1 SimpleOrderItemRequest.
     * - Không cho phép null hoặc list rỗng (validation nên kiểm tra ở BE).
     */
    private List<SimpleOrderItemRequest> items;
}
