-- V37__add_notification_rule_settings.sql
-- ------------------------------------------------------------------
-- Mục đích:
--  - Thêm các key cấu hình cho Phase 4.3 (Notification Center + Rule Engine)
--  - Cho phép bật/tắt rule tạo thông báo theo event + scheduled rule
--  - Dùng ON CONFLICT DO NOTHING để tránh lỗi khi migrate lại
-- ------------------------------------------------------------------

INSERT INTO system_setting (setting_group, setting_key, setting_value, value_type, description)
VALUES
-- ================================================================
-- GROUP: NOTIFICATION (Bật/tắt toàn bộ + rule theo sự kiện)
-- ================================================================
('NOTIFICATION', 'notification.enabled', 'true', 'BOOLEAN', 'Bật/tắt toàn bộ hệ thống thông báo'),

('NOTIFICATION', 'notification.rule.order_created', 'true', 'BOOLEAN', 'Tạo thông báo khi tạo order'),
('NOTIFICATION', 'notification.rule.payment_created', 'true', 'BOOLEAN', 'Tạo thông báo khi tạo thanh toán'),

-- ================================================================
-- RULE: LOW STOCK (cảnh báo tồn kho thấp)
-- ================================================================
('NOTIFICATION', 'notification.rule.low_stock.enabled', 'true', 'BOOLEAN', 'Bật/tắt cảnh báo tồn kho thấp'),
('NOTIFICATION', 'notification.rule.low_stock.threshold', '10', 'NUMBER', 'Ngưỡng tồn kho thấp (<= ngưỡng sẽ cảnh báo)'),

-- ================================================================
-- RULE: REVENUE ZERO (cảnh báo X ngày không có doanh thu)
-- ================================================================
('NOTIFICATION', 'notification.rule.revenue_zero.enabled', 'false', 'BOOLEAN', 'Bật/tắt cảnh báo không có doanh thu'),
('NOTIFICATION', 'notification.rule.revenue_zero.days', '1', 'NUMBER', 'Số ngày liên tiếp không có doanh thu để cảnh báo')

ON CONFLICT (setting_key) DO NOTHING;
