package com.restaurant.api.repository;

import com.restaurant.api.entity.RecipeItem;
import com.restaurant.api.entity.Dish;
import com.restaurant.api.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * RecipeItemRepository – Repository thao tác với bảng recipe_item
 * ------------------------------------------------------------------
 * Chức năng chính:
 *  - Lấy danh sách định lượng theo món ăn (dish)
 *  - Kiểm tra trùng nguyên liệu trong cùng một món
 *  - Xóa toàn bộ định lượng của 1 món (dùng khi reset recipe)
 *
 * Áp dụng:
 *  - Rule 13: comment tiếng Việt
 *  - Rule 26: đúng kiểu dữ liệu Entity/Id
 */
public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long> {

    /**
     * Lấy toàn bộ danh sách định lượng theo món ăn.
     *
     * @param dish Món ăn cần lấy định lượng
     * @return Danh sách RecipeItem của món đó
     */
    List<RecipeItem> findByDish(Dish dish);

    /**
     * Tìm 1 định lượng cụ thể theo:
     *  - Món ăn
     *  - Nguyên liệu
     * Dùng để kiểm tra xem món này đã có nguyên liệu đó chưa
     * → tránh thêm trùng.
     *
     * @param dish       Món ăn
     * @param ingredient Nguyên liệu
     * @return Optional<RecipeItem>
     */
    Optional<RecipeItem> findByDishAndIngredient(Dish dish, Ingredient ingredient);

    /**
     * Xóa toàn bộ định lượng của 1 món ăn.
     * Dùng trong trường hợp:
     *  - Reset lại recipe cho món đó.
     *
     * @param dish Món ăn cần xóa định lượng
     */
    void deleteByDish(Dish dish);

    /**
     * Lấy toàn bộ định lượng theo dishId
     */
    List<RecipeItem> findByDishId(Long dishId);
}
