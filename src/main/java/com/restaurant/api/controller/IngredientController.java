package com.restaurant.api.controller;

import com.restaurant.api.dto.ingredient.*;
import com.restaurant.api.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IngredientController – API quản lý nguyên liệu.
 * ---------------------------------------------------------
 * Gồm các chức năng:
 *  - Lấy danh sách nguyên liệu
 *  - Tạo mới nguyên liệu
 *  - Cập nhật nguyên liệu
 *  - Xóa nguyên liệu (xóa mềm)
 * ---------------------------------------------------------
 * Tất cả comment đều viết tiếng Việt theo Rule 13.
 */
@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    /**
     * API lấy danh sách toàn bộ nguyên liệu.
     */
    @GetMapping
    public ResponseEntity<List<IngredientResponse>> getAllIngredients() {
        return ResponseEntity.ok(ingredientService.getAll());
    }

    /**
     * API tạo mới nguyên liệu.
     */
    @PostMapping
    public ResponseEntity<IngredientResponse> createIngredient(
            @Valid @RequestBody IngredientCreateRequest req
    ) {
        return ResponseEntity.ok(ingredientService.create(req));
    }

    /**
     * API cập nhật nguyên liệu.
     */
    @PutMapping("/{id}")
    public ResponseEntity<IngredientResponse> updateIngredient(
            @PathVariable Long id,
            @Valid @RequestBody IngredientUpdateRequest req
    ) {
        return ResponseEntity.ok(ingredientService.update(id, req));
    }

    /**
     * API xóa nguyên liệu (xóa mềm).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
