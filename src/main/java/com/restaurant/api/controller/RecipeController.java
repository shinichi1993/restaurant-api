package com.restaurant.api.controller;

import com.restaurant.api.dto.recipe.RecipeItemRequest;
import com.restaurant.api.dto.recipe.RecipeItemResponse;
import com.restaurant.api.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RecipeController – API RESTful cho Module 07: Định lượng món (Recipe)
 * -------------------------------------------------------------------------
 * Chức năng:
 *  - Lấy danh sách định lượng của 1 món ăn
 *  - Thêm mới 1 dòng định lượng (nguyên liệu + số lượng)
 *  - Cập nhật 1 dòng định lượng
 *  - Xóa 1 dòng định lượng
 *  - Reset toàn bộ định lượng của 1 món
 *
 * Đường dẫn gốc: /api/recipes
 * Áp dụng:
 *  - Rule 9: API RESTful, rõ ràng, đúng method
 *  - Rule 13: Comment tiếng Việt đầy đủ
 *  - Rule 26: Dùng DTO vào/ra, không trả Entity trực tiếp
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * API lấy danh sách định lượng theo món ăn.
     * -----------------------------------------------------------------
     * URL ví dụ:
     *  - GET /api/recipes/dish/1
     *
     * @param dishId ID món ăn
     * @return Danh sách định lượng (list RecipeItemResponse)
     */
    @GetMapping("/dish/{dishId}")
    public ResponseEntity<List<RecipeItemResponse>> getByDish(
            @PathVariable Long dishId
    ) {
        return ResponseEntity.ok(recipeService.getByDish(dishId));
    }

    /**
     * API thêm mới 1 dòng định lượng cho món ăn.
     * -----------------------------------------------------------------
     * URL:
     *  - POST /api/recipes/add
     *
     * Request body (JSON):
     *  {
     *    "dishId": 1,
     *    "ingredientId": 3,
     *    "quantity": 120.000
     *  }
     *
     * @param req RecipeItemRequest (đã validate)
     * @return RecipeItemResponse vừa tạo
     */
    @PostMapping("/add")
    public ResponseEntity<RecipeItemResponse> add(
            @Valid @RequestBody RecipeItemRequest req
    ) {
        return ResponseEntity.ok(recipeService.add(req));
    }

    /**
     * API cập nhật 1 dòng định lượng.
     * -----------------------------------------------------------------
     * URL:
     *  - PUT /api/recipes/update/{id}
     *
     * @param id  ID dòng định lượng cần cập nhật
     * @param req Dữ liệu mới (dishId, ingredientId, quantity)
     * @return RecipeItemResponse sau khi cập nhật
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<RecipeItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RecipeItemRequest req
    ) {
        return ResponseEntity.ok(recipeService.update(id, req));
    }

    /**
     * API xóa 1 dòng định lượng (xóa 1 nguyên liệu khỏi recipe của món).
     * -----------------------------------------------------------------
     * URL:
     *  - DELETE /api/recipes/delete/{id}
     *
     * @param id ID dòng định lượng cần xóa
     * @return Thông báo OK
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ) {
        recipeService.delete(id);
        return ResponseEntity.ok("Xóa định lượng thành công");
    }

    /**
     * API reset toàn bộ định lượng của 1 món.
     * -----------------------------------------------------------------
     * URL:
     *  - DELETE /api/recipes/reset/{dishId}
     *
     * Hành vi:
     *  - Xóa toàn bộ các dòng recipe_item thuộc về dishId đó
     *
     * @param dishId ID món ăn cần reset định lượng
     * @return Thông báo OK
     */
    @DeleteMapping("/reset/{dishId}")
    public ResponseEntity<String> resetByDish(
            @PathVariable Long dishId
    ) {
        recipeService.resetByDish(dishId);
        return ResponseEntity.ok("Reset định lượng món thành công");
    }
}
