package com.restaurant.api.dto.table;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PosTableStatusResponse
 * ------------------------------------------------------------
 * DTO trả về thông tin BÀN + ORDER hiện tại cho màn hình POS.
 *
 * Mỗi bàn hiển thị:
 *  - tableId, tableName, status (AVAILABLE / OCCUPIED / DISABLED / MERGED)
 *
 * Nếu bàn có order đang mở (NEW / SERVING):
 *  - orderId, orderCode, orderCreatedAt
 *
 * Thống kê món:
 *  - totalItems   : tổng số món trong order
 *  - newItems     : số món NEW
 *  - cookingItems : số món COOKING
 *  - doneItems    : số món DONE
 *
 * Cờ:
 *  - waitingForPayment : true nếu order đang ở trạng thái SERVING
 */
@Data
@Builder
public class PosTableStatusResponse {

    private Long tableId;
    private String tableName;
    private String status;

    private Integer capacity;

    private Long orderId;
    private String orderCode;
    private LocalDateTime orderCreatedAt;

    private Long totalItems;
    private Long newItems;
    private Long cookingItems;
    private Long doneItems;

    private boolean waitingForPayment;
}
