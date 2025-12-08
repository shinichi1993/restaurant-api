package com.restaurant.api.controller;

import com.restaurant.api.dto.kitchen.KitchenItemResponse;
import com.restaurant.api.dto.kitchen.KitchenOrderResponse;
import com.restaurant.api.dto.kitchen.UpdateKitchenItemStatusRequest;
import com.restaurant.api.enums.OrderItemStatus;
import com.restaurant.api.service.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KitchenController
 * ------------------------------------------------------------
 * API cho MÀN HÌNH BẾP (Kitchen Display).
 *
 * Các chức năng chính:
 *  - GET  /api/kitchen/items
 *      → Lấy danh sách món cần chế biến
 *
 *  - PUT  /api/kitchen/items/{orderItemId}/status
 *      → Cập nhật trạng thái 1 món (NEW → SENT_TO_KITCHEN → COOKING → DONE)
 */
@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenService kitchenService;

    // =====================================================================
    // 1. LẤY DANH SÁCH MÓN CHO BẾP
    // =====================================================================

    /**
     * API lấy danh sách món hiển thị trên màn hình bếp.
     * ------------------------------------------------------------
     * Query param:
     *  - status (optional): nếu truyền → lọc theo đúng status
     *                       nếu không truyền → mặc định NEW, SENT_TO_KITCHEN, COOKING
     *
     * Ví dụ:
     *  - GET /api/kitchen/items
     *  - GET /api/kitchen/items?status=COOKING
     */
    @GetMapping("/items")
    public ResponseEntity<List<KitchenItemResponse>> getKitchenItems(
            @RequestParam(required = false) OrderItemStatus status
    ) {
        List<KitchenItemResponse> items = kitchenService.getKitchenItems(status);
        return ResponseEntity.ok(items);
    }

    // =====================================================================
    // 2. CẬP NHẬT TRẠNG THÁI 1 MÓN
    // =====================================================================

    /**
     * API cập nhật trạng thái 1 món từ màn hình bếp.
     * ------------------------------------------------------------
     * URL:
     *  - PUT /api/kitchen/items/{orderItemId}/status
     *
     * Body (JSON):
     *  {
     *      "newStatus": "COOKING",
     *      "note": "Bắt đầu chế biến"
     *  }
     *
     * @param orderItemId ID bản ghi OrderItem
     * @param req         trạng thái mới + ghi chú
     */
    @PutMapping("/items/{orderItemId}/status")
    public ResponseEntity<KitchenItemResponse> updateKitchenItemStatus(
            @PathVariable Long orderItemId,
            @RequestBody UpdateKitchenItemStatusRequest req
    ) {
        KitchenItemResponse updated = kitchenService.updateItemStatus(orderItemId, req);
        return ResponseEntity.ok(updated);
    }

    /**
     * API lấy toàn bộ ORDER kèm danh sách món cho màn hình bếp.
     * ------------------------------------------------------------------
     * URL: GET /api/kitchen/orders
     * Input: không cần param (filter sẽ để BE tự xử lý theo trạng thái nếu cần)
     * Output: List<KitchenOrderResponse>
     *
     * Lý do không dùng API cũ:
     *  - API cũ trả danh sách OrderItem (món) rời rạc → UI rất khó nhóm theo bàn/order
     *  - KDS thực tế luôn group theo ORDER (bàn, orderCode, danh sách món)
     * ------------------------------------------------------------------
     */
    @GetMapping("/orders")
    public ResponseEntity<List<KitchenOrderResponse>> getKitchenOrders() {
        return ResponseEntity.ok(kitchenService.getKitchenOrders());
    }
}
