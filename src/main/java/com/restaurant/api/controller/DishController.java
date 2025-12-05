package com.restaurant.api.controller;

import com.restaurant.api.dto.dish.DishRequest;
import com.restaurant.api.dto.dish.DishResponse;
import com.restaurant.api.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DishController – API RESTful quản lý MÓN ĂN.
 * ------------------------------------------------------------------
 * Chức năng cung cấp cho FE:
 *  - Lấy danh sách tất cả món ăn
 *  - Lọc món theo danh mục (categoryId)
 *  - Tạo mới món ăn
 *  - Cập nhật món
 *  - Xóa món (xóa mềm – đổi status sang INACTIVE)
 *
 * Quy tắc:
 *  - Toàn bộ nghiệp vụ đặt trong DishService (Rule 28)
 *  - Controller chỉ làm nhiệm vụ nhận request / trả response
 *  - Dùng DTO DishRequest / DishResponse (Rule 26)
 *  - Comment tiếng Việt (Rule 13)
 * ------------------------------------------------------------------
 */
@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    // ==========================================================
    // 1. GET /api/dishes – Lấy danh sách tất cả món ăn
    // ==========================================================

    /**
     * API lấy toàn bộ món ăn trong hệ thống.
     * Dùng cho:
     *  - Trang quản lý món ăn (DishPage)
     *  - Các module khác cần danh sách món (Order, Recipe...)
     */
    @GetMapping
    public ResponseEntity<List<DishResponse>> getAll() {
        return ResponseEntity.ok(dishService.getAll());
    }

    // ==========================================================
    // 2. GET /api/dishes/by-category/{categoryId}
    // ==========================================================

    /**
     * API lấy danh sách món ăn theo danh mục.
     * Dùng cho:
     *  - Filter trên FE: chọn Category → load món tương ứng
     *  - Module Recipe: chọn món theo Category
     *
     * @param categoryId ID danh mục món ăn
     */
    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<DishResponse>> getByCategory(
            @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(dishService.getByCategory(categoryId));
    }

    // ==========================================================
    // 3. POST /api/dishes – Tạo mới món ăn
    // ==========================================================

    /**
     * API tạo mới một món ăn.
     * Yêu cầu:
     *  - Body làm theo DishRequest
     *  - Có validate @Valid (Rule 26)
     */
    @PostMapping
    public ResponseEntity<DishResponse> create(
            @Valid @RequestBody DishRequest req
    ) {
        return ResponseEntity.ok(dishService.create(req));
    }

    // ==========================================================
    // 4. PUT /api/dishes/{id} – Cập nhật món ăn
    // ==========================================================

    /**
     * API cập nhật thông tin món ăn.
     *
     * @param id  ID món ăn cần sửa
     * @param req Dữ liệu cập nhật (DishRequest)
     */
    @PutMapping("/{id}")
    public ResponseEntity<DishResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DishRequest req
    ) {
        return ResponseEntity.ok(dishService.update(id, req));
    }

    // ==========================================================
    // 5. DELETE /api/dishes/{id} – Xóa món (xóa mềm)
    // ==========================================================

    /**
     * API xóa món ăn (xóa mềm).
     * Thực tế sẽ:
     *  - Không xóa bản ghi trong DB
     *  - Chỉ đổi trạng thái sang INACTIVE
     *
     * Lý do:
     *  - Đảm bảo các Order / Invoice cũ vẫn tham chiếu được món này.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        dishService.delete(id);
        return ResponseEntity.ok("Xóa món ăn thành công");
    }
}
