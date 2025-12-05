-- V5__create_stock_entry_table.sql
-- Tạo bảng lưu lịch sử nhập kho / điều chỉnh kho nguyên liệu

CREATE TABLE stock_entry (
    id BIGSERIAL PRIMARY KEY,
    ingredient_id BIGINT NOT NULL,
    quantity NUMERIC(12, 2) NOT NULL, -- Số lượng nhập (có thể âm nếu điều chỉnh)
    note TEXT,                        -- Ghi chú (ví dụ: nhập mới / điều chỉnh)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_stock_entry_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredient(id)
);
