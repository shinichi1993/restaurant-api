package com.restaurant.api.controller;

import com.restaurant.api.dto.order.*;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderController
 * ------------------------------------------------------------
 * API quản lý ĐƠN GỌI MÓN (Module 08)
 *
 * Các chức năng:
 *  - Tạo order mới (POST /api/orders)
 *  - Lấy danh sách order (GET /api/orders)
 *  - Lấy chi tiết order (GET /api/orders/{id})
 *  - Đổi trạng thái order (PUT /api/orders/{id}/status)
 *  - Xóa order chưa thanh toán (DELETE /api/orders/{id})
 *
 * Ghi chú:
 *  - Trừ/hoàn kho xử lý ở OrderService
 *  - Thanh toán & hóa đơn sẽ ở Module 09–10
 * ------------------------------------------------------------
 * Toàn bộ comment theo Rule 13: tiếng Việt đầy đủ.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ============================================================
    // 1) TẠO ORDER MỚI
    // ============================================================

    /**
     * API tạo đơn gọi món
     * ------------------------------------------------------------
     * - FE gửi danh sách món (dishId + quantity)
     * - Hệ thống tính tổng tiền, lưu order, order_item
     * - Tự động trừ kho theo RecipeItem
     *
     * @param principal        dữ liệu món khách gọi
     * @param req       user đăng nhập (dùng để lấy createdBy)
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            java.security.Principal principal,
            @Valid @RequestBody OrderCreateRequest req
    ) {
        // 👉 Lấy username trực tiếp từ JWT
        String username = principal.getName();

        // 🟢 Gọi service xử lý
        OrderResponse response = orderService.createOrder(req, username);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // 2) LẤY DANH SÁCH ORDER (filter theo status + ngày)
    // ============================================================

    /**
     * API lấy danh sách order theo điều kiện:
     *  - status: lọc theo trạng thái
     *  - from / to: lọc theo ngày tạo
     *  - nếu không truyền → trả về toàn bộ
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime to
    ) {
        List<OrderResponse> list = orderService.getOrders(status, from, to);
        return ResponseEntity.ok(list);
    }

    // ============================================================
    // 3) LẤY CHI TIẾT 1 ORDER
    // ============================================================

    /**
     * API xem chi tiết 1 order (bao gồm danh sách món)
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    // ============================================================
    // 4) CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
    // ============================================================

    /**
     * API đổi trạng thái order
     * ------------------------------------------------------------
     * Quy tắc hợp lệ:
     *  - NEW → SERVING / CANCELED
     *  - SERVING → PAID / CANCELED
     *
     * Nếu chuyển sang CANCELED → tự hoàn kho.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        orderService.updateStatus(id, status);
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    // ============================================================
    // 5) XÓA ORDER (CHỈ CHO PHÉP NEW / SERVING)
    // ============================================================

    /**
     * API xóa order
     * ------------------------------------------------------------
     * - Chỉ cho phép xóa order khi:
     *      + status = NEW
     *      + status = SERVING
     * - Khi xóa → tự động hoàn kho
     * - Không cho phép xóa khi order đã thanh toán
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok("Xóa order thành công");
    }

    /**
     * API lấy order đang mở của một bàn.
     * ---------------------------------------------------------
     * GET /api/orders/by-table/{tableId}
     *
     * Dùng cho POS:
     *  - Khi mở màn hình order của 1 bàn trên tablet
     *  - FE gọi API này để kiểm tra:
     *      + Nếu có order đang mở → trả về OrderResponse
     *      + Nếu không có order → trả về 200 + body = null
     *
     * Ví dụ:
     *  - GET /api/orders/by-table/5
     */
    @GetMapping("/by-table/{tableId}")
    public ResponseEntity<OrderResponse> getOrderByTable(@PathVariable Long tableId) {
        OrderResponse response = orderService.getOrderByTableId(tableId);
        return ResponseEntity.ok(response);
    }

    /**
     * API cập nhật lại danh sách món của order.
     * ------------------------------------------------------------
     * URL: PUT /api/orders/{orderId}
     *
     * Dùng trong POS:
     *  - Khi bàn đã có order chưa thanh toán
     *  - Nhân viên thêm/xoá/chỉnh số lượng món xong → gửi lại lên BE
     *
     * Quy tắc:
     *  - Không được sửa order đã thanh toán (PAID)
     *  - Không được sửa order đã hủy (CANCELED)
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> updateItems(
            @PathVariable Long orderId,
            @RequestBody OrderUpdateRequest req
    ) {
        return ResponseEntity.ok(orderService.updateOrderItems(orderId, req.getItems()));
    }

    /**
     * API tạo đơn gọi món cho chế độ Simple POS
     * ------------------------------------------------------------
     * URL:
     *   - POST /api/orders/simple-create
     *
     * Chức năng:
     *   - Dùng cho chế độ POS đơn giản (simple_pos_mode = true)
     *   - Luồng xử lý nhanh:
     *        + Chọn bàn (tùy setting)
     *        + Chọn món
     *        + Bấm thanh toán → thanh toán luôn
     *
     * Khác biệt với API createOrder:
     *   - Request dùng SimpleOrderRequest (tableId + items đơn giản)
     *   - Không xử lý logic nâng cao (update món, gửi bếp...),
     *     chỉ tạo order + order_item cơ bản.
     */
    @PostMapping("/simple-create")
    public ResponseEntity<OrderResponse> createSimpleOrder(
            java.security.Principal principal,
            @Valid @RequestBody SimpleOrderRequest req
    ) {
        // 👉 Lấy username từ JWT (giống createOrder)
        String username = principal.getName();

        // 👉 Gọi service xử lý luồng Simple POS
        OrderResponse response = orderService.simpleCreate(req, username);

        return ResponseEntity.ok(response);
    }

}
