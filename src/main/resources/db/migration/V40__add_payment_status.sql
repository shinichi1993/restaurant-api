-- V40__add_payment_status.sql
-- =========================================================
-- Thêm cột status cho bảng payment để chuẩn bị online payment
-- OFFLINE sẽ mặc định SUCCESS
-- =========================================================

ALTER TABLE payment
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';

COMMENT ON COLUMN payment.status IS
'Trạng thái thanh toán: OFFLINE mặc định SUCCESS; ONLINE sẽ dùng PENDING/SUCCESS/FAILED/CANCELED';
