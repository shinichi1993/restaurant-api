-- V10__create_invoice_tables.sql
-- =====================================================================
-- Flyway V10 – Tạo bảng HÓA ĐƠN cho hệ thống nhà hàng
-- Gồm:
--   - Bảng invoice      : Lưu thông tin hóa đơn thanh toán cho 1 order
--   - Bảng invoice_item : Lưu chi tiết từng món trong hóa đơn
-- ---------------------------------------------------------------------
-- Quy ước:
--   - Đặt tên bảng, cột dạng snake_case
--   - Khoá ngoại tham chiếu:
--       + invoice.order_id      -> orders.id
--       + invoice_item.invoice_id -> invoice.id
--       + invoice_item.dish_id    -> dish.id
--   - Các trường số tiền dùng NUMERIC để map với BigDecimal trên BE
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Tạo bảng invoice – Thông tin hóa đơn
-- ---------------------------------------------------------------------
CREATE TABLE invoice (
    id              BIGSERIAL PRIMARY KEY,         -- Khóa chính

    order_id        BIGINT       NOT NULL,         -- ID đơn hàng gốc
    total_amount    NUMERIC(18,2) NOT NULL,        -- Tổng tiền hóa đơn (sau giảm giá, nếu có)

    payment_method  VARCHAR(50),                   -- Phương thức thanh toán (CASH, CARD, MOMO...)
    paid_at         TIMESTAMP,                     -- Thời gian thanh toán (khi hóa đơn được tạo)

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(), -- Thời gian tạo bản ghi
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()  -- Thời gian cập nhật bản ghi gần nhất
);

-- Tạo khóa ngoại: invoice.order_id -> orders.id
ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT;
        -- Không cho xóa order nếu đã có invoice, để đảm bảo lịch sử

-- Tạo index để truy vấn nhanh hóa đơn theo order
CREATE INDEX idx_invoice_order_id ON invoice(order_id);

-- ---------------------------------------------------------------------
-- 2. Tạo bảng invoice_item – Chi tiết từng món trong hóa đơn
-- ---------------------------------------------------------------------
CREATE TABLE invoice_item (
    id           BIGSERIAL PRIMARY KEY,          -- Khóa chính

    invoice_id   BIGINT       NOT NULL,          -- ID hóa đơn cha
    dish_id      BIGINT       NOT NULL,          -- ID món ăn (tham chiếu bảng dish)

    dish_name    VARCHAR(255) NOT NULL,          -- Tên món tại thời điểm in hóa đơn (snapshot)
    dish_price   NUMERIC(18,2) NOT NULL,         -- Đơn giá tại thời điểm in hóa đơn

    quantity     INT          NOT NULL,          -- Số lượng món trong hóa đơn
    subtotal     NUMERIC(18,2) NOT NULL,         -- Thành tiền = dish_price * quantity

    created_at   TIMESTAMP NOT NULL DEFAULT NOW() -- Thời gian tạo bản ghi
);

-- Khóa ngoại: invoice_item.invoice_id -> invoice.id
ALTER TABLE invoice_item
    ADD CONSTRAINT fk_invoice_item_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoice(id)
        ON DELETE CASCADE;
        -- Nếu xóa invoice thì xóa luôn các invoice_item liên quan

-- Khóa ngoại: invoice_item.dish_id -> dish.id
ALTER TABLE invoice_item
    ADD CONSTRAINT fk_invoice_item_dish
        FOREIGN KEY (dish_id)
        REFERENCES dish(id)
        ON DELETE RESTRICT;
        -- Không cho xóa dish nếu còn lịch sử hóa đơn liên quan

-- Index để truy vấn nhanh danh sách item theo invoice_id
CREATE INDEX idx_invoice_item_invoice_id ON invoice_item(invoice_id);

-- Index phụ nếu cần thống kê theo dish
CREATE INDEX idx_invoice_item_dish_id ON invoice_item(dish_id);

-- ---------------------------------------------------------------------
-- 3. Trigger đơn giản tự update updated_at khi UPDATE invoice
--    (Optional – có thể dùng ở BE nếu muốn, nhưng thêm sẵn cho tiện)
-- ---------------------------------------------------------------------

CREATE OR REPLACE FUNCTION trg_invoice_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_invoice_before_update
BEFORE UPDATE ON invoice
FOR EACH ROW
EXECUTE FUNCTION trg_invoice_set_updated_at();

-- =====================================================================
-- Kết thúc Flyway V10 – Tạo bảng invoice + invoice_item
-- =====================================================================
