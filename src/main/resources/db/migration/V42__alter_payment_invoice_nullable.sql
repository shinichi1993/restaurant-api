-- =========================================================
-- ONLINE PAYMENT SUPPORT (MoMo, VNPAY...)
-- Cho phép payment ONLINE tồn tại trước khi có invoice
-- =========================================================

ALTER TABLE payment
ALTER COLUMN invoice_id DROP NOT NULL;
