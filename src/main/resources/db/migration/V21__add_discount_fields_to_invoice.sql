-- ============================================================
--  V18__add_discount_fields_to_invoice.sql
--  Thêm các cột liên quan tới Voucher vào bảng invoice:
--    - discount_amount: Số tiền được giảm
--    - voucher_code:    Mã voucher áp dụng cho hóa đơn
-- ============================================================

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(18,2);

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(50);

-- ============================================================
-- Ghi chú:
--  - discount_amount:
--      + Nếu hóa đơn không áp dụng voucher có thể để NULL hoặc 0.
--  - voucher_code:
--      + Lưu lại mã voucher đã áp dụng để tra cứu sau này.
-- ============================================================
