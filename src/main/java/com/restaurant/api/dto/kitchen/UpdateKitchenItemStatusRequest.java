package com.restaurant.api.dto.kitchen;

import com.restaurant.api.enums.OrderItemStatus;
import lombok.Data;

/**
 * UpdateKitchenItemStatusRequest
 * ------------------------------------------------------------
 * Body gửi từ FE (màn hình bếp) khi cập nhật trạng thái 1 món.
 *
 * Ví dụ:
 *  - NEW  → SENT_TO_KITCHEN
 *  - SENT_TO_KITCHEN → COOKING
 *  - COOKING → DONE
 *  - *CANCELED*      → hủy món (nếu hệ thống cho phép)
 */
@Data
public class UpdateKitchenItemStatusRequest {

    /**
     * Trạng thái mới mà bếp muốn set cho món.
     */
    private OrderItemStatus newStatus;

    /**
     * Ghi chú (tuỳ chọn), ví dụ: "Hết nguyên liệu", "Khách đổi món"...
     */
    private String note;
}
