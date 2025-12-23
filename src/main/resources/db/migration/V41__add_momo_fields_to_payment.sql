-- ======================================================================
-- ADD MoMo fields for ONLINE payment trace + idempotency
-- ======================================================================

ALTER TABLE payment
    ADD COLUMN momo_order_id VARCHAR(200),
    ADD COLUMN momo_request_id VARCHAR(200),
    ADD COLUMN momo_trans_id BIGINT,
    ADD COLUMN momo_result_code INT,
    ADD COLUMN momo_message TEXT,
    ADD COLUMN momo_pay_type VARCHAR(50),
    ADD COLUMN momo_response_time BIGINT,
    ADD COLUMN momo_extra_data TEXT;

-- Lưu link trả về từ MoMo để FE hiển thị/redirect
ALTER TABLE payment
    ADD COLUMN momo_pay_url TEXT,
    ADD COLUMN momo_qr_code_url TEXT,
    ADD COLUMN momo_deeplink TEXT;

-- Index để query nhanh theo momo_order_id (idempotent)
CREATE INDEX IF NOT EXISTS idx_payment_momo_order_id ON payment(momo_order_id);
