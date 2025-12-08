-- V26__insert_key_into_systemsetting.sql
-- ------------------------------------------------------------------
-- Mục đích:
--  - Thêm key invoice.print_layout vào bảng system_setting
-- ------------------------------------------------------------------

INSERT INTO system_setting (setting_group, setting_key, setting_value, value_type, description)
VALUES('POS', 'pos.simple_pos_mode','false','BOOLEAN','Chế độ POS đơn giản'),
('POS', 'pos.simple_pos_require_table','false','BOOLEAN','Chế độ POS đơn giản có bàn ăn hay không'),
('POS', 'pos.auto_order_serving_on_item_cooking','false','BOOLEAN','Có tự động chuyển status order khi có món chuyển sang COOK hay không')
ON CONFLICT (setting_key) DO NOTHING;
-- ON CONFLICT để tránh lỗi nếu migrate lại trên môi trường đã có dữ liệu