-- =============================================
-- BẢNG DISH – DANH MỤC MÓN ĂN
-- Module 06 – Category & Dish
-- =============================================

CREATE TABLE dish (
    id BIGSERIAL PRIMARY KEY,

    -- Tên món ăn
    name VARCHAR(255) NOT NULL,

    -- Thuộc danh mục nào
    category_id BIGINT NOT NULL,

    -- Giá bán (Rule 26: BigDecimal)
    price NUMERIC(18,2) NOT NULL,

    -- Ảnh món ăn (URL)
    image_url TEXT,

    -- Trạng thái hoạt động của món
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / INACTIVE

    -- Ngày tạo và cập nhật
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Ràng buộc FK đến category
    CONSTRAINT fk_dish_category FOREIGN KEY (category_id)
        REFERENCES category(id) ON DELETE RESTRICT
);

-- =============================================
-- TRIGGER cập nhật updated_at tự động
-- =============================================

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp_dish
BEFORE UPDATE ON dish
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();
