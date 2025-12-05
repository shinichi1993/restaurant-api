-- ============================================================
-- V19__add_table_id_to_orders.sql
-- MODULE 16 – TABLE MANAGEMENT (Quản lý bàn)
-- ------------------------------------------------------------
-- Mục đích:
--   - Bổ sung cột table_id vào bảng orders
--   - Liên kết đơn gọi món với bàn (restaurant_table)
-- ============================================================

-- Thêm cột table_id cho bảng orders
ALTER TABLE orders
ADD COLUMN table_id BIGINT NULL;

-- Tạo khóa ngoại liên kết với bảng restaurant_table
ALTER TABLE orders
ADD CONSTRAINT fk_orders_table
FOREIGN KEY (table_id) REFERENCES restaurant_table(id);

-- Index để phục vụ truy vấn order theo bàn
CREATE INDEX idx_orders_table_id ON orders(table_id);
