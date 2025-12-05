-- V11__create_payment_table.sql
-- ============================================================
-- MỤC ĐÍCH:
--   - Tạo bảng payment để lưu thông tin THANH TOÁN cho Order
--   - Mỗi record = 1 lần thanh toán cho 1 order
--   - Liên kết với:
--        + orders   (đơn hàng)
--        + invoice  (hóa đơn đã tạo ở Module 09)
--
-- CHUẨN HÓA DỮ LIỆU:
--   - amount        dùng NUMERIC(18,2) để map với BigDecimal (Rule 26)
--   - method        lưu dạng chuỗi: CASH / BANK / MOMO / ... (sẽ map với enum PaymentMethod bên BE)
--   - paid_at       thời gian thanh toán
--   - created_by    ID user thực hiện thanh toán (app_user.id)
--
-- LƯU Ý:
--   - Tên bảng: payment
--   - Tên bảng orders giả định là "orders"
--   - Tên bảng invoice giả định là "invoice"
--     → Nếu trong DB của bạn khác (vd: "order", "invoices") thì sửa lại 2 dòng FOREIGN KEY tương ứng.
-- ============================================================

CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,                -- Khóa chính

    order_id   BIGINT NOT NULL,              -- FK tới đơn hàng được thanh toán
    invoice_id BIGINT NOT NULL,              -- FK tới hóa đơn tương ứng

    amount NUMERIC(18,2) NOT NULL,           -- Số tiền thanh toán (theo đơn vị VNĐ)
    method VARCHAR(50) NOT NULL,             -- Phương thức thanh toán (CASH / BANK / MOMO / ...)

    note TEXT,                               -- Ghi chú thêm (optional)

    paid_at    TIMESTAMP NOT NULL DEFAULT NOW(), -- Thời gian thực hiện thanh toán
    created_by BIGINT,                           -- User thực hiện thanh toán (tham chiếu app_user.id)

    created_at TIMESTAMP NOT NULL DEFAULT NOW(), -- Thời gian tạo record
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()  -- Thời gian cập nhật record
);

-- Ràng buộc khóa ngoại tới bảng orders
ALTER TABLE payment
    ADD CONSTRAINT fk_payment_order
    FOREIGN KEY (order_id)
    REFERENCES orders (id);

-- Ràng buộc khóa ngoại tới bảng invoice
ALTER TABLE payment
    ADD CONSTRAINT fk_payment_invoice
    FOREIGN KEY (invoice_id)
    REFERENCES invoice (id);

-- (OPTIONAL) Nếu muốn ràng buộc created_by tới app_user:
-- ALTER TABLE payment
--     ADD CONSTRAINT fk_payment_user
--     FOREIGN KEY (created_by)
--     REFERENCES app_user (id);

-- Thêm index để tối ưu truy vấn theo order, invoice và ngày thanh toán
CREATE INDEX idx_payment_order_id ON payment(order_id);
CREATE INDEX idx_payment_invoice_id ON payment(invoice_id);
CREATE INDEX idx_payment_paid_at ON payment(paid_at);
