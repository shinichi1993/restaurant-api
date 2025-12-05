-- ============================================================
-- V8__create_recipe_item_table.sql
-- MODULE 07 – RECIPE (ĐỊNH LƯỢNG MÓN)
-- ------------------------------------------------------------
-- Mục đích:
--   - Lưu định lượng nguyên liệu cho từng món ăn (Dish)
--   - Mỗi dòng tương ứng 1 nguyên liệu dùng cho 1 món
--   - Dùng cho các module sau (Order, Stock, Report...)
--
-- Cấu trúc bảng:
--   - recipe_item:
--       + id           : khóa chính
--       + dish_id      : FK tới bảng dish
--       + ingredient_id: FK tới bảng ingredient
--       + quantity     : số lượng nguyên liệu dùng cho 1 đơn vị món
--       + created_at   : ngày tạo
--       + updated_at   : ngày cập nhật
--
-- Quy ước:
--   - quantity dùng kiểu NUMERIC(12,3) để hỗ trợ gram/ml chính xác
--   - Một cặp (dish_id, ingredient_id) chỉ xuất hiện 1 lần
--     → tránh trùng định lượng cho cùng một nguyên liệu trong 1 món
-- ============================================================

CREATE TABLE recipe_item (
    id BIGSERIAL PRIMARY KEY,

    -- Món ăn (Dish) sử dụng định lượng này
    dish_id BIGINT NOT NULL,

    -- Nguyên liệu (Ingredient) được dùng trong món
    ingredient_id BIGINT NOT NULL,

    -- Số lượng nguyên liệu dùng cho 1 phần món
    -- Ví dụ: 120.000 gram, 0.500 lít, 1.000 cái...
    quantity NUMERIC(12,3) NOT NULL,

    -- Thời gian tạo / cập nhật bản ghi
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Ràng buộc khóa ngoại tới bảng dish
    CONSTRAINT fk_recipe_item_dish
        FOREIGN KEY (dish_id) REFERENCES dish(id)
        ON DELETE CASCADE,

    -- Ràng buộc khóa ngoại tới bảng ingredient
    CONSTRAINT fk_recipe_item_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredient(id)
        ON DELETE RESTRICT,

    -- Không cho phép trùng 1 nguyên liệu nhiều lần trong cùng 1 món
    CONSTRAINT uq_recipe_item_dish_ingredient
        UNIQUE (dish_id, ingredient_id)
);

-- ============================================================
-- TRIGGER cập nhật updated_at tự động khi UPDATE
-- ------------------------------------------------------------
-- Lưu ý:
--   - Hàm trigger_set_timestamp() đã được tạo ở các version trước
--   - Tại đây chỉ tạo trigger cho bảng recipe_item
-- ============================================================

CREATE TRIGGER set_timestamp_recipe_item
BEFORE UPDATE ON recipe_item
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();
