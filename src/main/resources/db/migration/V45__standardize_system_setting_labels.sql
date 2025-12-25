-- =====================================================================
-- V45__standardize_system_setting_labels.sql
-- ---------------------------------------------------------------------
-- Chuẩn hóa:
--  - Label hiển thị cho người dùng
--  - Thứ tự hiển thị (order_index)
--  - Input type (SWITCH / NUMBER / INPUT / SELECT)
-- ---------------------------------------------------------------------
-- Sau bước này:
--  - FE render đẹp, không còn label = setting_key
--  - Không cần sửa FE thêm khi thêm key mới

-- INSERT INTO system_setting
-- (setting_group, setting_key, setting_value, value_type, label, input_type, order_index)
-- VALUES
--('POS', 'pos.new_feature_enabled', 'false', 'BOOLEAN',
-- 'Bật tính năng POS mới', 'SWITCH', 99);
-- =====================================================================

-- =========================
-- 1️⃣ THÔNG TIN NHÀ HÀNG
-- =========================
UPDATE system_setting SET
    label = 'Tên nhà hàng',
    order_index = 1
WHERE setting_key = 'restaurant.name';

UPDATE system_setting SET
    label = 'Địa chỉ nhà hàng',
    order_index = 2
WHERE setting_key = 'restaurant.address';

UPDATE system_setting SET
    label = 'Số điện thoại liên hệ',
    order_index = 3
WHERE setting_key = 'restaurant.phone';

UPDATE system_setting SET
    label = 'Mã số thuế',
    order_index = 4
WHERE setting_key = 'restaurant.tax_id';


-- =========================
-- 2️⃣ HÓA ĐƠN & THUẾ
-- =========================
UPDATE system_setting SET
    label = 'Thuế VAT mặc định (%)',
    input_type = 'NUMBER',
    min_value = 0,
    max_value = 100,
    order_index = 1
WHERE setting_key = 'vat.rate';

UPDATE system_setting SET
    label = 'Layout in hóa đơn',
    input_type = 'SELECT',
    order_index = 2
WHERE setting_key = 'invoice.print_layout';


-- =========================
-- 3️⃣ LOYALTY (TÍCH ĐIỂM)
-- =========================
UPDATE system_setting SET
    label = 'Bật tính năng Loyalty',
    input_type = 'SWITCH',
    order_index = 1
WHERE setting_key = 'loyalty.enabled';

UPDATE system_setting SET
    label = 'Tỷ lệ tích điểm (điểm / 1.000đ)',
    input_type = 'NUMBER',
    min_value = 0,
    order_index = 2
WHERE setting_key = 'loyalty.earn_rate';

UPDATE system_setting SET
    label = 'Bật dùng điểm (Redeem)',
    input_type = 'SWITCH',
    order_index = 3
WHERE setting_key = 'loyalty.redeem.enabled';

UPDATE system_setting SET
    label = 'Giá trị tiền cho 1 điểm (VNĐ)',
    input_type = 'NUMBER',
    min_value = 0,
    order_index = 4
WHERE setting_key = 'loyalty.redeem.rate';

UPDATE system_setting SET
    label = 'Tỷ lệ tối đa được redeem (%)',
    input_type = 'NUMBER',
    min_value = 0,
    max_value = 100,
    order_index = 5
WHERE setting_key = 'loyalty.redeem.max_percent';


-- =========================
-- 4️⃣ CẤU HÌNH POS
-- =========================
UPDATE system_setting SET
    label = 'Tự động gửi order xuống bếp',
    input_type = 'SWITCH',
    order_index = 1
WHERE setting_key = 'pos.auto_send_kitchen';

UPDATE system_setting SET
    label = 'Cho phép hủy món sau khi order',
    input_type = 'SWITCH',
    order_index = 2
WHERE setting_key = 'pos.allow_cancel_item';

UPDATE system_setting SET
    label = 'Cho phép sửa món sau khi gửi bếp',
    input_type = 'SWITCH',
    order_index = 3
WHERE setting_key = 'pos.allow_edit_after_send';

UPDATE system_setting SET
    label = 'Thời gian refresh POS (giây)',
    input_type = 'NUMBER',
    min_value = 0,
    max_value = 300,
    order_index = 4
WHERE setting_key = 'pos.refresh_interval_sec';

UPDATE system_setting SET
    label = 'Tự chuyển order sang SERVING khi món bắt đầu COOKING',
    input_type = 'SWITCH',
    order_index = 5
WHERE setting_key = 'pos.auto_order_serving_on_item_cooking';

UPDATE system_setting SET
    label = 'Bật chế độ POS đơn giản (Simple POS)',
    input_type = 'SWITCH',
    order_index = 6
WHERE setting_key = 'pos.simple_pos_mode';

UPDATE system_setting SET
    label = 'Trong Simple POS: bắt buộc chọn bàn',
    input_type = 'SWITCH',
    order_index = 7
WHERE setting_key = 'pos.simple_pos_require_table';


-- =========================
-- 5️⃣ GIẢM GIÁ & BÁO CÁO
-- =========================
UPDATE system_setting SET
    label = 'Giảm giá mặc định (%)',
    input_type = 'NUMBER',
    min_value = 0,
    max_value = 100,
    order_index = 1
WHERE setting_key = 'discount.default_percent';

UPDATE system_setting SET
    label = 'Giảm giá tối đa cho hóa đơn (%)',
    input_type = 'NUMBER',
    min_value = 0,
    max_value = 100,
    order_index = 2
WHERE setting_key = 'discount.max_percent';

UPDATE system_setting SET
    label = 'Cho phép dùng giảm giá mặc định cùng voucher',
    input_type = 'SWITCH',
    order_index = 3
WHERE setting_key = 'discount.allow_with_voucher';

UPDATE system_setting SET
    label = 'Bật giảm giá mặc định',
    input_type = 'SWITCH',
    order_index = 4
WHERE setting_key = 'discount.use_default';

UPDATE system_setting SET
    label = 'Định dạng export báo cáo mặc định',
    input_type = 'SELECT',
    order_index = 5
WHERE setting_key = 'report.default_export';

UPDATE system_setting SET
    label = 'Footer mặc định cho file PDF',
    input_type = 'INPUT',
    order_index = 6
WHERE setting_key = 'report.pdf_footer';

UPDATE system_setting SET
    label = 'Hiển thị logo trên báo cáo PDF',
    input_type = 'SWITCH',
    order_index = 7
WHERE setting_key = 'report.pdf_show_logo';


-- =========================
-- 6️⃣ NOTIFICATION / RULE ENGINE
-- =========================
UPDATE system_setting SET
    label = 'Bật hệ thống thông báo',
    input_type = 'SWITCH',
    order_index = 1
WHERE setting_key = 'notification.enabled';

UPDATE system_setting SET
    label = 'Thông báo khi tạo order',
    input_type = 'SWITCH',
    order_index = 2
WHERE setting_key = 'notification.rule.order_created';

UPDATE system_setting SET
    label = 'Thông báo khi tạo thanh toán',
    input_type = 'SWITCH',
    order_index = 3
WHERE setting_key = 'notification.rule.payment_created';

UPDATE system_setting SET
    label = 'Cảnh báo tồn kho thấp',
    input_type = 'SWITCH',
    order_index = 4
WHERE setting_key = 'notification.rule.low_stock.enabled';

UPDATE system_setting SET
    label = 'Ngưỡng tồn kho thấp',
    input_type = 'NUMBER',
    min_value = 0,
    order_index = 5
WHERE setting_key = 'notification.rule.low_stock.threshold';

UPDATE system_setting SET
    label = 'Cảnh báo không có doanh thu',
    input_type = 'SWITCH',
    order_index = 6
WHERE setting_key = 'notification.rule.revenue_zero.enabled';

UPDATE system_setting SET
    label = 'Số ngày liên tiếp không có doanh thu để cảnh báo',
    input_type = 'NUMBER',
    min_value = 1,
    order_index = 7
WHERE setting_key = 'notification.rule.revenue_zero.days';
