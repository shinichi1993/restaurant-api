-- V26__insert_key_into_systemsetting.sql
-- ------------------------------------------------------------------
-- Mục đích:
--  - Thêm key invoice.print_layout vào bảng system_setting
-- ------------------------------------------------------------------

INSERT INTO system_setting (setting_group, setting_key, setting_value, value_type, description)
VALUES('INVOICE', 'invoice.print_layout','A5','STRING','Kiểu dạng layout PDF khi export')
ON CONFLICT (setting_key) DO NOTHING;
-- ON CONFLICT để tránh lỗi nếu migrate lại trên môi trường đã có dữ liệu