-- V24__add_loyalty_point_to_invoice.sql
-- ------------------------------------------------------------------
-- Mục đích:
--  - Thêm cột lưu số điểm tích lũy (loyalty) khách nhận được
--  - Dữ liệu này gắn cứng với hóa đơn tại thời điểm thanh toán
--  - Không phụ thuộc vào thay đổi cấu hình loyalty sau này
-- ------------------------------------------------------------------

ALTER TABLE invoice
ADD COLUMN loyalty_earned_point INTEGER DEFAULT 0;
