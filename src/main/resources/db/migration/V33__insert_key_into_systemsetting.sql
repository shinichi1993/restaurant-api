-- V33__insert_key_into_systemsetting.sql
-- ------------------------------------------------------------------
-- Mục đích:
--  - Thêm key redeem của loyalty vào bảng system_setting
-- ------------------------------------------------------------------

INSERT INTO system_setting (setting_group, setting_key, setting_value, value_type, description)
VALUES('LOYALTY', 'loyalty.redeem.enabled','true','BOOLEAN','Bật/tắt dùng điểm'),
('LOYALTY', 'loyalty.redeem.rate','1','NUMBER','Bao nhiêu điểm = 1.000đ'),
('LOYALTY', 'loyalty.redeem.max_percent','100','NUMBER','Tối đa % hóa đơn được redeem'),
('LOYALTY', 'loyalty.redeem.min_point','0','NUMBER','Số điểm tối thiểu được dùng')
ON CONFLICT (setting_key) DO NOTHING;
-- ON CONFLICT để tránh lỗi nếu migrate lại trên môi trường đã có dữ liệu