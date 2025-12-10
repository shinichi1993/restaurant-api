-- V31__add_invoice_snapshot_fields_to_invoice.sql
-- ------------------------------------------------------------
-- Thêm các cột snapshot tiền vào bảng invoice
--  - original_total_amount      : Tổng tiền gốc trước giảm
--  - voucher_discount_amount    : Giảm do voucher
--  - default_discount_amount    : Giảm do discount mặc định
--  - amount_before_vat          : Sau giảm, trước VAT
--  - vat_rate                   : % VAT snapshot
--  - vat_amount                 : Số tiền VAT snapshot
--  - customer_paid              : Tiền khách trả
--  - change_amount              : Tiền thừa trả lại khách
-- ------------------------------------------------------------

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS original_total_amount NUMERIC(18, 0),
    ADD COLUMN IF NOT EXISTS voucher_discount_amount NUMERIC(18, 0),
    ADD COLUMN IF NOT EXISTS default_discount_amount NUMERIC(18, 0),
    ADD COLUMN IF NOT EXISTS amount_before_vat NUMERIC(18, 0),
    ADD COLUMN IF NOT EXISTS vat_rate NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS vat_amount NUMERIC(18, 0),
    ADD COLUMN IF NOT EXISTS customer_paid NUMERIC(18, 0),
    ADD COLUMN IF NOT EXISTS change_amount NUMERIC(18, 0);

-- ------------------------------------------------------------
-- Fill dữ liệu mặc định cho các invoice cũ (Option A)
--  - original_total_amount  = total_amount hiện tại
--  - voucher_discount_amount = discount_amount (coi như toàn bộ là voucher)
--  - default_discount_amount = 0
--  - amount_before_vat      = total_amount
--  - vat_rate               = 0
--  - vat_amount             = 0
--  - customer_paid / change_amount = NULL (vì lúc đó chưa lưu)
-- ------------------------------------------------------------
UPDATE invoice
SET
    original_total_amount   = COALESCE(original_total_amount, total_amount),
    voucher_discount_amount = COALESCE(voucher_discount_amount, COALESCE(discount_amount, 0)),
    default_discount_amount = COALESCE(default_discount_amount, 0),
    amount_before_vat       = COALESCE(amount_before_vat, total_amount),
    vat_rate                = COALESCE(vat_rate, 0),
    vat_amount              = COALESCE(vat_amount, 0)
WHERE original_total_amount IS NULL;
