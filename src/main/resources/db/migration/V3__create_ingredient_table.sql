-- V4__create_ingredient_table.sql
-- Tạo bảng lưu nguyên liệu

CREATE TABLE ingredient (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(100) NOT NULL,   -- đơn vị tính (gram, ml, cái…)
    stock_quantity NUMERIC(12, 2) NOT NULL DEFAULT 0, -- tồn kho
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
