-- =====================================================================
-- V27__alter_order_item_for_pos_phase2.sql
-- Phase 2 – POS Advanced
-- ---------------------------------------------------------------------
-- Mục đích:
--   - Nâng cấp bảng order_item để hỗ trợ tính năng:
--       + Trạng thái chế biến món (Kitchen)
--       + Snapshot giá món tại thời điểm order
--       + Ghi chú món (ít cay, không hành...)
--
-- Lưu ý:
--   - Không xóa dữ liệu cũ
--   - Không dùng DEFAULT CURRENT_TIMESTAMP vì bảng cũ đã có trigger thời gian
--   - Thêm cột nếu chưa tồn tại (để tránh lỗi khi migrate lại)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Thêm cột snapshot_price (giá món tại thời điểm order)
-- ---------------------------------------------------------------------
ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS snapshot_price NUMERIC(18,2);

-- Với dữ liệu cũ → tạm gán snapshot_price = 0
-- (Phase 2 sau này có thể update lại theo dish.price nếu muốn)
UPDATE order_item
SET snapshot_price = 0
WHERE snapshot_price IS NULL;

-- ---------------------------------------------------------------------
-- 2) Thêm cột status (trạng thái món)
-- ---------------------------------------------------------------------
ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS status VARCHAR(40);

-- Gán mặc định cho dữ liệu cũ: tất cả món cũ = NEW
UPDATE order_item
SET status = 'NEW'
WHERE status IS NULL;

-- ---------------------------------------------------------------------
-- 3) Thêm cột note (ghi chú món)
-- ---------------------------------------------------------------------
ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS note TEXT;

-- Không cần update default cho note → NULL là hợp lệ
-- ---------------------------------------------------------------------
