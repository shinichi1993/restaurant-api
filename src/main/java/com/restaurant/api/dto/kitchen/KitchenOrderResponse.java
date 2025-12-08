package com.restaurant.api.dto.kitchen;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO đại diện cho 1 ORDER trong màn hình bếp.
 * --------------------------------------------------------------
 * Một ORDER hiển thị:
 *  - Thông tin bàn (tableName)
 *  - Mã đơn (orderCode)
 *  - Thời gian tạo (createdAt)
 *  - Danh sách món cần chế biến (items)
 * --------------------------------------------------------------
 */

@Data
@Builder
public class KitchenOrderResponse {

    private Long orderId;            // ID order để thao tác
    private String orderCode;        // Mã order hiển thị cho bếp
    private String tableName;        // Tên bàn: "Bàn 2", "Mang đi", ...

    private LocalDateTime createdAt; // Thời điểm order được tạo → dùng tính số phút

    private List<KitchenItemResponse> items;
    // Danh sách món thuộc order này (được nhóm theo order)
}
