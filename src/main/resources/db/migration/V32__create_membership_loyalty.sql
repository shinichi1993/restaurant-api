-- V32__create_membership_loyalty.sql
-- ==========================================================
-- PHASE 3 – MEMBERSHIP & LOYALTY
-- 1) Tạo bảng member
-- 2) Tạo bảng member_point_history
-- 3) Thêm cột member_id vào orders
-- ==========================================================

-- 1) BẢNG MEMBER – Lưu thông tin hội viên
CREATE TABLE IF NOT EXISTS member (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255),
    phone           VARCHAR(50) UNIQUE,
    email           VARCHAR(255),
    birthday        DATE,
    tier            VARCHAR(50),          -- BRONZE / SILVER / GOLD / PLATINUM
    total_point     INT NOT NULL DEFAULT 0,  -- Điểm khả dụng
    lifetime_point  INT NOT NULL DEFAULT 0,  -- Tổng điểm đã tích
    used_point      INT NOT NULL DEFAULT 0,  -- Tổng điểm đã dùng
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2) BẢNG MEMBER_POINT_HISTORY – Lịch sử cộng/trừ điểm
CREATE TABLE IF NOT EXISTS member_point_history (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL,
    change_amount   INT NOT NULL,          -- + điểm hoặc - điểm
    balance_after   INT NOT NULL,          -- tổng điểm sau thay đổi
    type            VARCHAR(50) NOT NULL,  -- EARN / REDEEM / ADJUST
    description     TEXT,
    order_id        BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_member_point_member_id
    ON member_point_history(member_id);

-- (Tuỳ chọn) nếu muốn FK thì thêm sau, hiện tại để lỏng tránh lỗi dữ liệu
-- ALTER TABLE member_point_history
--   ADD CONSTRAINT fk_member_point_member
--   FOREIGN KEY (member_id) REFERENCES member(id);

-- 3) THÊM CỘT member_id VÀO orders
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS member_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_orders_member_id
    ON orders(member_id);
