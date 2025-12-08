package com.restaurant.api.controller;

import com.restaurant.api.dto.table.*;
import com.restaurant.api.service.RestaurantTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TableController – API quản lý bàn (Module 16).
 * --------------------------------------------------
 * Dùng cho FE TablePage:
 * - CRUD bàn
 * - Gộp bàn, tách bàn, chuyển bàn
 */
@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final RestaurantTableService restaurantTableService;

    /**
     * API tạo bàn mới.
     */
    @PostMapping
    public ResponseEntity<TableResponse> createTable(@RequestBody TableRequest request) {
        TableResponse response = restaurantTableService.createTable(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API cập nhật thông tin bàn.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TableResponse> updateTable(
            @PathVariable Long id,
            @RequestBody TableRequest request
    ) {
        TableResponse response = restaurantTableService.updateTable(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * API xóa bàn.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        restaurantTableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API lấy danh sách tất cả bàn.
     */
    @GetMapping
    public ResponseEntity<List<TableResponse>> getAllTables() {
        List<TableResponse> list = restaurantTableService.getAllTables();
        return ResponseEntity.ok(list);
    }

    /**
     * API lấy chi tiết 1 bàn.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TableResponse> getTableById(@PathVariable Long id) {
        TableResponse response = restaurantTableService.getTableById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * API gộp 2 bàn.
     */
    @PostMapping("/merge")
    public ResponseEntity<Void> mergeTables(@RequestBody MergeTableRequest request) {
        restaurantTableService.mergeTables(request);
        return ResponseEntity.ok().build();
    }

    /**
     * API tách bàn (hủy trạng thái MERGED → AVAILABLE).
     */
    @PostMapping("/split/{id}")
    public ResponseEntity<Void> splitTable(@PathVariable Long id) {
        restaurantTableService.splitTable(id);
        return ResponseEntity.ok().build();
    }

    /**
     * API chuyển order từ bàn này sang bàn khác.
     */
    @PostMapping("/change")
    public ResponseEntity<Void> changeTable(@RequestBody ChangeTableRequest request) {
        restaurantTableService.changeTable(request);
        return ResponseEntity.ok().build();
    }

    /**
     * API cập nhật trạng thái bàn (dùng khi cần chỉnh tay).
     */
    @PostMapping("/status")
    public ResponseEntity<Void> updateTableStatus(@RequestBody UpdateTableStatusRequest request) {
        restaurantTableService.updateTableStatus(request);
        return ResponseEntity.ok().build();
    }

    /**
     * API trả về danh sách bàn dành riêng cho POS TABLE PAGE.
     * --------------------------------------------------------------
     * Mỗi phần tử bao gồm:
     *  - Thông tin bàn (id, name, status)
     *  - Nếu có order mở → kèm orderId, orderCode, orderCreatedAt
     *  - Thống kê món trong order: totalItems, newItems, cookingItems, doneItems
     *  - waitingForPayment = true nếu order.status = SERVING
     *
     * URL: GET /api/tables/pos-status
     */
    @GetMapping("/pos-status")
    public ResponseEntity<List<PosTableStatusResponse>> getPosTableStatuses() {
        List<PosTableStatusResponse> list = restaurantTableService.getPosTableStatuses();
        return ResponseEntity.ok(list);
    }

}
