-- ============================================================
--  V20__create_voucher_table.sql
--  Tạo bảng voucher dùng cho Module 17 – Mã giảm giá
--  Bao gồm các thông tin: loại giảm giá, giá trị giảm,
--  điều kiện áp dụng, thời gian hiệu lực và giới hạn sử dụng.
-- ============================================================

CREATE TABLE IF NOT EXISTS voucher (
    id BIGSERIAL PRIMARY KEY,                 -- Khoá chính tự tăng

    code VARCHAR(50) NOT NULL UNIQUE,         -- Mã voucher (duy nhất)

    description VARCHAR(255),                 -- Mô tả thông tin voucher

    discount_type VARCHAR(20) NOT NULL,       -- Loại giảm giá: PERCENT / FIXED
    discount_value DECIMAL(18,2) NOT NULL,    -- % giảm hoặc số tiền giảm cố định

    min_order_amount DECIMAL(18,2) DEFAULT 0, -- Giá trị đơn hàng tối thiểu để được áp dụng
    max_discount_amount DECIMAL(18,2),        -- Giảm tối đa (chỉ dùng khi discount_type = PERCENT)

    usage_limit INT NOT NULL DEFAULT 0,       -- Số lần được phép sử dụng voucher
    used_count INT NOT NULL DEFAULT 0,        -- Số lần đã sử dụng

    start_date TIMESTAMP NOT NULL,            -- Ngày bắt đầu hiệu lực
    end_date TIMESTAMP NOT NULL,              -- Ngày kết thúc hiệu lực

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / INACTIVE

    created_at TIMESTAMP DEFAULT NOW(),        -- Thời điểm tạo voucher
    updated_at TIMESTAMP DEFAULT NOW()         -- Thời điểm cập nhật cuối
);

-- ============================================================
--  Ghi chú:
--  - code UNIQUE để chống trùng mã giảm giá
--  - discount_value phải > 0 (validate xử lý ở BE)
--  - max_discount_amount có thể NULL đối với loại FIXED
--  - usage_limit = số lần tối đa, used_count = số lần đã dùng
--  - status: ACTIVE / INACTIVE (giá trị EXPIRED xử lý runtime)
-- ============================================================
