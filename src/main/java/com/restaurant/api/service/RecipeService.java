package com.restaurant.api.service;

import com.restaurant.api.dto.recipe.RecipeItemRequest;
import com.restaurant.api.dto.recipe.RecipeItemResponse;
import com.restaurant.api.entity.Dish;
import com.restaurant.api.entity.Ingredient;
import com.restaurant.api.entity.RecipeItem;
import com.restaurant.api.repository.DishRepository;
import com.restaurant.api.repository.IngredientRepository;
import com.restaurant.api.repository.RecipeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RecipeService – Xử lý nghiệp vụ định lượng món ăn (Recipe)
 * ------------------------------------------------------------------
 * Chức năng chính:
 *  - Lấy danh sách định lượng theo món ăn (dish)
 *  - Thêm mới 1 dòng định lượng (nguyên liệu + quantity)
 *  - Cập nhật định lượng
 *  - Xóa 1 dòng định lượng
 *  - Reset toàn bộ định lượng của 1 món
 *
 * Module này sẽ được sử dụng bởi:
 *  - FE RecipePage (Module 07)
 *  - Module Order/Stock trong tương lai để trừ kho theo định lượng
 *
 * Áp dụng:
 *  - Rule 13: comment tiếng Việt đầy đủ
 *  - Rule 26: kiểu dữ liệu chuẩn (BigDecimal, LocalDateTime...)
 */
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeItemRepository recipeItemRepository;
    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;

    /**
     * Lấy danh sách định lượng nguyên liệu cho một món ăn.
     * ------------------------------------------------------------------
     * Bước xử lý:
     *  1. Tìm Dish theo dishId
     *  2. Lấy toàn bộ RecipeItem theo Dish
     *  3. Convert sang DTO RecipeItemResponse trả về cho FE
     *
     * @param dishId ID món ăn
     * @return Danh sách định lượng (list DTO RecipeItemResponse)
     */
    public List<RecipeItemResponse> getByDish(Long dishId) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        List<RecipeItem> items = recipeItemRepository.findByDish(dish);

        return items.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Thêm mới 1 dòng định lượng cho món ăn.
     * ------------------------------------------------------------------
     * Bước xử lý:
     *  1. Lấy Dish theo dishId trong request
     *  2. Lấy Ingredient theo ingredientId trong request
     *  3. Kiểm tra xem món này đã có dòng định lượng với nguyên liệu đó chưa
     *     - Nếu đã tồn tại → báo lỗi "Nguyên liệu đã tồn tại trong định lượng món"
     *  4. Tạo mới RecipeItem và lưu DB
     *  5. Trả về DTO RecipeItemResponse
     *
     * @param req DTO RecipeItemRequest từ FE
     * @return DTO RecipeItemResponse vừa tạo
     */
    public RecipeItemResponse add(RecipeItemRequest req) {
        // 1. Lấy món ăn
        Dish dish = dishRepository.findById(req.getDishId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        // 2. Lấy nguyên liệu
        Ingredient ingredient = ingredientRepository.findById(req.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        // 3. Kiểm tra trùng (mỗi món + 1 nguyên liệu chỉ được xuất hiện 1 lần)
        recipeItemRepository.findByDishAndIngredient(dish, ingredient)
                .ifPresent(item -> {
                    throw new RuntimeException("Nguyên liệu này đã tồn tại trong định lượng của món");
                });

        // 4. Tạo mới bản ghi định lượng
        RecipeItem item = RecipeItem.builder()
                .dish(dish)
                .ingredient(ingredient)
                .quantity(req.getQuantity())
                .build();

        recipeItemRepository.save(item);

        // 5. Trả về DTO
        return toResponse(item);
    }

    /**
     * Cập nhật 1 dòng định lượng.
     * ------------------------------------------------------------------
     * Cho phép:
     *  - Đổi nguyên liệu (ingredientId)
     *  - Đổi món (dishId) nếu cần
     *  - Đổi quantity
     *
     * Lưu ý:
     *  - Vẫn đảm bảo không trùng (dish + ingredient) sau khi cập nhật
     *
     * @param id  ID dòng định lượng cần cập nhật
     * @param req Dữ liệu mới từ FE
     * @return DTO RecipeItemResponse sau khi cập nhật
     */
    public RecipeItemResponse update(Long id, RecipeItemRequest req) {
        // Lấy bản ghi cũ
        RecipeItem item = recipeItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy định lượng"));

        // Lấy món ăn mới
        Dish dish = dishRepository.findById(req.getDishId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        // Lấy nguyên liệu mới
        Ingredient ingredient = ingredientRepository.findById(req.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        // Kiểm tra xem sau khi cập nhật có trùng với dòng khác không
        recipeItemRepository.findByDishAndIngredient(dish, ingredient)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Nguyên liệu này đã tồn tại trong định lượng của món");
                    }
                });

        // Cập nhật dữ liệu
        item.setDish(dish);
        item.setIngredient(ingredient);
        item.setQuantity(req.getQuantity());

        recipeItemRepository.save(item);

        return toResponse(item);
    }

    /**
     * Xóa 1 dòng định lượng (1 nguyên liệu của món).
     * ------------------------------------------------------------------
     *
     * @param id ID dòng định lượng cần xóa
     */
    public void delete(Long id) {
        RecipeItem item = recipeItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy định lượng"));

        recipeItemRepository.delete(item);
    }

    /**
     * Reset toàn bộ định lượng của 1 món:
     * ------------------------------------------------------------------
     * - Xóa hết tất cả RecipeItem thuộc món đó
     * - Dùng khi:
     *    + Muốn định nghĩa lại toàn bộ recipe cho món
     *    + Hoặc khi món đổi công thức hoàn toàn
     *
     * @param dishId ID món ăn cần reset định lượng
     */
    @Transactional
    public void resetByDish(Long dishId) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        recipeItemRepository.deleteByDish(dish);
    }

    /**
     * Hàm tiện ích: Convert Entity → DTO RecipeItemResponse.
     * ------------------------------------------------------------------
     * Dùng chung cho tất cả các API trả dữ liệu định lượng ra FE.
     *
     * @param item Entity RecipeItem
     * @return DTO RecipeItemResponse
     */
    private RecipeItemResponse toResponse(RecipeItem item) {
        return RecipeItemResponse.builder()
                .id(item.getId())
                .ingredientId(item.getIngredient() != null ? item.getIngredient().getId() : null)
                .ingredientName(item.getIngredient() != null ? item.getIngredient().getName() : null)
                .unit(item.getIngredient() != null ? item.getIngredient().getUnit() : null)
                .quantity(item.getQuantity())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
