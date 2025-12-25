-- =========================================================
-- V46__add_setting_group_label_and_seed_vi.sql
-- ---------------------------------------------------------
-- Mục đích:
--  - Thêm cột setting_group_label để hiển thị tên tab tiếng Việt
--  - Giữ setting_group làm KEY logic (POS, LOYALTY...)
--  - Tránh hard-code label ở FE
-- =========================================================

-- 1️⃣ Thêm cột label cho group setting
ALTER TABLE system_setting
ADD COLUMN setting_group_label VARCHAR(255);

-- =========================================================
-- 2️⃣ Seed label tiếng Việt cho các group hiện có
-- ---------------------------------------------------------
-- Chỉ update các record CHƯA có label
-- (để tránh ghi đè nếu sau này đã chỉnh tay)
-- =========================================================

UPDATE system_setting
SET setting_group_label = 'Thông tin nhà hàng'
WHERE setting_group = 'RESTAURANT'
  AND setting_group_label IS NULL;

UPDATE system_setting
SET setting_group_label = 'Hóa đơn & Thuế'
WHERE setting_group = 'INVOICE'
  AND setting_group_label IS NULL;

UPDATE system_setting
SET setting_group_label = 'Loyalty (Tích điểm)'
WHERE setting_group = 'LOYALTY'
  AND setting_group_label IS NULL;

UPDATE system_setting
SET setting_group_label = 'Cấu hình POS'
WHERE setting_group = 'POS'
  AND setting_group_label IS NULL;

UPDATE system_setting
SET setting_group_label = 'Giảm giá'
WHERE setting_group = 'DISCOUNT'
  AND setting_group_label IS NULL;

UPDATE system_setting
SET setting_group_label = 'Báo cáo'
WHERE setting_group = 'REPORT'
  AND setting_group_label IS NULL;

UPDATE system_setting
SET setting_group_label = 'Thông báo'
WHERE setting_group = 'NOTIFICATION'
  AND setting_group_label IS NULL;

