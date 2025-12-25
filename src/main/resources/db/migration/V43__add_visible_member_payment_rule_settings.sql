-- V43__add_visible_member_payment_rule_settings.sql
-- ------------------------------------------------------------------
-- Mục đích:
--  - Cho phép bật/tắt rule ẩn hiện Nhập thông tin hội viên khi thanh toán
--  - Dùng ON CONFLICT DO NOTHING để tránh lỗi khi migrate lại
-- ------------------------------------------------------------------

INSERT INTO system_setting (
    setting_group,
    setting_key,
    setting_value,
    value_type,
    description
)
SELECT
    'LOYALTY',
    'loyalty.member_in_payment_enabled',
    'false',
    'BOOLEAN',
    'Bật/tắt hiển thị nhập hội viên khi thanh toán'
WHERE NOT EXISTS (
    SELECT 1 FROM system_setting
    WHERE setting_key = 'loyalty.member_in_payment_enabled'
);