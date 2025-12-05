-- V15__create_notification_tables.sql
-- ==================================================================
-- Tạo 2 bảng phục vụ Module 14 – Notification:
--   1) notification              : Lưu nội dung thông báo (dùng chung)
--   2) notification_user_status  : Trạng thái đọc theo từng user
--
-- Quy ước:
--   - Thời gian dùng TIMESTAMP (khớp LocalDateTime bên BE)
--   - Khóa ngoại trỏ tới bảng app_user (user hệ thống)
--   - Đảm bảo mỗi (notification_id, user_id) chỉ có 1 dòng trạng thái
-- ==================================================================

-- --------------------------------------------------------------
-- 1. Bảng notification – lưu nội dung thông báo
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification (
    id           BIGSERIAL PRIMARY KEY,         -- Khóa chính
    title        VARCHAR(255) NOT NULL,         -- Tiêu đề ngắn gọn
    message      TEXT         NOT NULL,         -- Nội dung chi tiết
    type         VARCHAR(50)  NOT NULL,         -- Loại thông báo: ORDER, STOCK, PAYMENT...
    link         VARCHAR(255),                  -- Link điều hướng FE (VD: /orders/123)
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW() -- Thời điểm tạo
);

-- Index để lọc nhanh theo loại + thời gian
CREATE INDEX IF NOT EXISTS idx_notification_type
    ON notification(type);

CREATE INDEX IF NOT EXISTS idx_notification_created_at
    ON notification(created_at);

-- --------------------------------------------------------------
-- 2. Bảng notification_user_status – trạng thái theo từng user
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_user_status (
    id              BIGSERIAL PRIMARY KEY,      -- Khóa chính

    notification_id BIGINT      NOT NULL,       -- FK tới notification.id
    user_id         BIGINT      NOT NULL,       -- FK tới app_user.id

    status          VARCHAR(20) NOT NULL DEFAULT 'UNREAD', -- UNREAD / READ
    read_at         TIMESTAMP,                  -- Thời điểm user đọc (nếu đã đọc)
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),    -- Thời điểm gán thông báo cho user

    CONSTRAINT fk_nus_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_nus_user
        FOREIGN KEY (user_id) REFERENCES app_user(id)
        ON DELETE CASCADE
);

-- Mỗi (notification, user) chỉ có 1 bản ghi trạng thái
CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_user_status
    ON notification_user_status(notification_id, user_id);

-- Index hỗ trợ đếm nhanh số thông báo chưa đọc theo user
CREATE INDEX IF NOT EXISTS idx_nus_user_status
    ON notification_user_status(user_id, status);

-- Index hỗ trợ sort/filter theo thời gian
CREATE INDEX IF NOT EXISTS idx_nus_created_at
    ON notification_user_status(created_at);
