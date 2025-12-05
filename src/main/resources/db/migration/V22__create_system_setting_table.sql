-- V22__create_system_setting_table.sql
-- Module 20 – Advanced Settings
-- ------------------------------------------------------------
-- Mục đích:
--  - Tạo bảng system_setting để lưu các cấu hình động của hệ thống
--  - Không sửa đổi schema các bảng cũ
--  - Các module khác (Order, Payment, POS, Report...) sẽ đọc cấu hình từ bảng này
-- ------------------------------------------------------------

-- 1) Tạo bảng system_setting
--    Dùng để lưu toàn bộ cấu hình theo dạng key-value
--    Ví dụ:
--      - restaurant.name           → Tên nhà hàng
--      - restaurant.phone          → Số điện thoại
--      - vat.rate                  → Thuế VAT (%)
--      - loyalty.enabled           → Bật/tắt tích điểm
--      - pos.auto_send_kitchen     → Tự động gửi order sang bếp
--      - report.default_export     → Định dạng export mặc định (PDF/EXCEL)

CREATE TABLE IF NOT EXISTS system_setting (
    id              BIGSERIAL PRIMARY KEY,

    -- Nhóm cấu hình (ví dụ: RESTAURANT, POS, LOYALTY, REPORT...)
    setting_group   VARCHAR(100) NOT NULL,

    -- Khóa cấu hình (dạng key duy nhất, ví dụ: restaurant.name, vat.rate)
    setting_key     VARCHAR(150) NOT NULL,

    -- Giá trị cấu hình lưu dạng text, BE sẽ tự parse sang kiểu tương ứng
    setting_value   TEXT NOT NULL,

    -- Kiểu dữ liệu logic của cấu hình, dùng ở BE để convert (STRING, NUMBER, BOOLEAN, JSON)
    value_type      VARCHAR(50) NOT NULL,

    -- Mô tả ý nghĩa của cấu hình, để hiển thị trên màn hình Settings
    description     TEXT,

    -- Thời gian tạo / cập nhật (theo chuẩn chung của dự án)
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,

    -- Người tạo / cập nhật (id user), để sau này có thể join nếu cần
    created_by      BIGINT,
    updated_by      BIGINT
);

-- 2) Tạo unique index cho setting_key
--    Đảm bảo mỗi key cấu hình chỉ tồn tại 1 lần trong hệ thống.
CREATE UNIQUE INDEX IF NOT EXISTS uq_system_setting_key
    ON system_setting (setting_key);

-- 3) (Tuỳ chọn) Tạo index cho setting_group để filter theo nhóm nhanh hơn
CREATE INDEX IF NOT EXISTS idx_system_setting_group
    ON system_setting (setting_group);

-- 4) Seed một số cấu hình mặc định ban đầu
--    Lưu ý:
--      - Các giá trị ban đầu giúp FE/BE không bị null khi mới triển khai
--      - Có thể sửa lại sau trong màn hình Settings

INSERT INTO system_setting (setting_group, setting_key, setting_value, value_type, description)
VALUES
    -- Nhóm cấu hình thông tin nhà hàng
    ('RESTAURANT', 'restaurant.name',         'Nhà hàng chưa cấu hình tên',   'STRING', 'Tên nhà hàng hiển thị trên hóa đơn và báo cáo'),
    ('RESTAURANT', 'restaurant.address',      'Chưa cấu hình địa chỉ',        'STRING', 'Địa chỉ nhà hàng in trên hóa đơn'),
    ('RESTAURANT', 'restaurant.phone',        '0000000000',                   'STRING', 'Số điện thoại liên hệ của nhà hàng'),
    ('RESTAURANT', 'restaurant.tax_id',       '',                             'STRING', 'Mã số thuế của nhà hàng (nếu có)'),

    -- Cấu hình VAT
    ('INVOICE',    'vat.rate',                '0',                            'NUMBER', 'Thuế VAT mặc định áp dụng cho hóa đơn, đơn vị: %'),

    -- Cấu hình Loyalty (tạm thay thế Module 18)
    ('LOYALTY',    'loyalty.enabled',         'false',                        'BOOLEAN','Bật/tắt chức năng tích điểm cho khách hàng'),
    ('LOYALTY',    'loyalty.earn_rate',       '0',                            'NUMBER', 'Số điểm tích được trên mỗi 1.000đ (hoặc đơn vị tiền tệ cấu hình)'),
    ('LOYALTY',    'loyalty.redeem_rate',     '0',                            'NUMBER', 'Giá trị tiền tương ứng với mỗi 1 điểm khi đổi điểm'),
    ('LOYALTY',    'loyalty.min_redeem_point','0',                            'NUMBER', 'Số điểm tối thiểu để được phép đổi'),

    -- Cấu hình POS
    ('POS',        'pos.auto_send_kitchen',   'false',                        'BOOLEAN','Nếu true: sau khi tạo order sẽ tự động gửi món xuống bếp'),
    ('POS',        'pos.allow_cancel_item',   'true',                         'BOOLEAN','Cho phép hủy món sau khi order'),
    ('POS',        'pos.allow_edit_after_send','true',                        'BOOLEAN','Cho phép sửa số lượng món sau khi đã gửi bếp'),
    ('POS',        'pos.refresh_interval_sec','15',                           'NUMBER', 'Thời gian (giây) tự động refresh dữ liệu trên màn hình POS'),

    -- Cấu hình giảm giá / voucher
    ('DISCOUNT',   'discount.default_percent','0',                            'NUMBER', 'Phần trăm giảm giá mặc định cho hóa đơn nếu không dùng voucher'),
    ('DISCOUNT',   'discount.max_percent',    '100',                          'NUMBER', 'Giới hạn % giảm tối đa cho 1 hóa đơn'),
    ('DISCOUNT',   'discount.allow_with_voucher','true',                      'BOOLEAN','Cho phép áp dụng giảm giá mặc định cùng với voucher'),

    -- Cấu hình báo cáo & export
    ('REPORT',     'report.default_export',   'PDF',                          'STRING', 'Định dạng export mặc định cho báo cáo: PDF hoặc EXCEL'),
    ('REPORT',     'report.pdf_footer',       'Cảm ơn Quý khách đã sử dụng dịch vụ!', 'STRING', 'Footer mặc định in dưới cuối file PDF báo cáo'),
    ('REPORT',     'report.pdf_show_logo',    'true',                         'BOOLEAN','Có hiển thị logo nhà hàng trên báo cáo PDF hay không')
ON CONFLICT (setting_key) DO NOTHING;
-- ON CONFLICT để tránh lỗi nếu migrate lại trên môi trường đã có dữ liệu
