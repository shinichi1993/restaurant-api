-- V23__add_discount_use_default_setting.sql
-- --------------------------------------------------------
-- Thêm cấu hình mới: discount.use_default
-- Dùng để BẬT / TẮT việc áp dụng giảm giá mặc định cho hóa đơn
-- true  = có áp dụng giảm giá mặc định (như hiện tại)
-- false = KHÔNG áp dụng giảm giá mặc định (kể cả khi default_percent > 0)
-- --------------------------------------------------------

INSERT INTO system_setting (
    setting_key,
    setting_value,
    setting_group,
    description,
    value_type
    -- Nếu bảng system_setting của bạn còn các cột khác (vd: created_at, updated_at, ...),
    -- hãy bổ sung đúng tên cột + giá trị mặc định tương ứng ở đây cho khớp schema hiện tại.
)
VALUES (
    'discount.use_default',
    'true',
    'DISCOUNT',
    'Bật tắt tính năng giảm giá mặc định cho hóa đơn',
    'BOOLEAN'
);
