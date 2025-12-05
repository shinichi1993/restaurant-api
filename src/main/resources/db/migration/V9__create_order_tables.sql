-- ============================================================
-- V9__create_order_tables.sql
-- MODULE 08 – ORDER (Gọi món)
-- ------------------------------------------------------------
-- Mục đích:
--   - Tạo bảng orders: lưu thông tin đơn gọi món
--   - Tạo bảng order_item: lưu danh sách món trong từng order
--
-- Ghi chú:
--   - Không dùng tên bảng "order" vì trùng từ khóa SQL → dùng "orders"
--   - Trạng thái đơn hàng: NEW / SERVING / PAID / CANCELED
--   - total_price: tổng tiền của order (chưa tính thuế/giảm giá nâng cao)
--   - created_by: liên kết user tạo đơn (app_user)
-- ============================================================

-- ============================================================
-- 1) BẢNG orders – Thông tin đơn gọi món
-- ============================================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,                  -- Khóa chính

    order_code VARCHAR(50) NOT NULL,           -- Mã đơn (hiển thị cho thu ngân/bếp)
                                               -- Có thể sinh dạng ORD20250101001

    total_price NUMERIC(12,2) NOT NULL DEFAULT 0,  -- Tổng tiền đơn (VNĐ)

    status VARCHAR(20) NOT NULL,               -- Trạng thái đơn:
                                               -- NEW / SERVING / PAID / CANCELED

    note TEXT,                                 -- Ghi chú thêm (tùy chọn: VD bàn số mấy, yêu cầu riêng)

    created_by BIGINT,                         -- ID user tạo đơn (tham chiếu app_user)

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),   -- Thời gian tạo
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()    -- Thời gian cập nhật gần nhất
);

-- Ràng buộc trạng thái hợp lệ
ALTER TABLE orders
ADD CONSTRAINT ck_orders_status
CHECK (status IN ('NEW', 'SERVING', 'PAID', 'CANCELED'));

-- Mã đơn không trùng nhau
ALTER TABLE orders
ADD CONSTRAINT uq_orders_order_code UNIQUE (order_code);

-- Khóa ngoại tới bảng app_user (Module 01)
ALTER TABLE orders
ADD CONSTRAINT fk_orders_created_by_user
FOREIGN KEY (created_by) REFERENCES app_user(id);

-- Index phục vụ lọc theo trạng thái + ngày tạo
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);


-- ============================================================
-- 2) BẢNG order_item – Danh sách món trong từng order
-- ============================================================
CREATE TABLE order_item (
    id BIGSERIAL PRIMARY KEY,              -- Khóa chính

    order_id BIGINT NOT NULL,              -- FK tới orders.id
    dish_id BIGINT NOT NULL,               -- FK tới dish.id (Module 06)

    quantity INT NOT NULL,                 -- Số lượng món (số phần gọi)

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Quantity phải > 0
ALTER TABLE order_item
ADD CONSTRAINT ck_order_item_quantity_positive
CHECK (quantity > 0);

-- Khóa ngoại tới orders
ALTER TABLE order_item
ADD CONSTRAINT fk_order_item_order
FOREIGN KEY (order_id) REFERENCES orders(id)
ON DELETE CASCADE;      -- Xóa order → xóa luôn các order_item

-- Khóa ngoại tới dish (Module 06)
ALTER TABLE order_item
ADD CONSTRAINT fk_order_item_dish
FOREIGN KEY (dish_id) REFERENCES dish(id)
ON DELETE RESTRICT;     -- Không cho xóa dish nếu đang được dùng trong order

-- Một món có thể xuất hiện nhiều lần trong order không?
-- Thường thì KHÔNG → mỗi dishId chỉ 1 dòng / order → dùng UNIQUE
ALTER TABLE order_item
ADD CONSTRAINT uq_order_item_order_dish
UNIQUE (order_id, dish_id);

-- Index phục vụ truy vấn nhanh theo order_id
CREATE INDEX idx_order_item_order_id ON order_item(order_id);


-- ============================================================
-- 3) TRIGGER CẬP NHẬT updated_at (nếu đã dùng từ các bảng trước)
-- ------------------------------------------------------------
-- Nếu trước đó bạn đã tạo hàm trigger_set_timestamp() dùng chung
-- cho các bảng (VD ở V3/V4/V5...), có thể dùng lại như sau:
-- ============================================================

CREATE TRIGGER set_timestamp_orders
BEFORE UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER set_timestamp_order_item
BEFORE UPDATE ON order_item
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();
