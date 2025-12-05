-- V16__create_audit_log_table.sql
-- =====================================================================
-- Tạo bảng audit_log để lưu lịch sử thao tác (Audit Log) toàn hệ thống
-- =====================================================================
-- Mục đích:
--   - Ghi lại các hành động quan trọng của người dùng:
--       + Tạo / sửa / xóa User, Role, Permission
--       + CRUD Nguyên liệu, Nhập kho, Món ăn, Định lượng
--       + Tạo / sửa / hủy Đơn hàng, Hóa đơn, Thanh toán
--       + Các sự kiện hệ thống khác (LOGIN, LOGOUT...)
--   - Phục vụ:
--       + Tra soát lỗi
--       + Kiểm tra thao tác người dùng
--       + Lịch sử thay đổi dữ liệu
--
-- Chuẩn hóa theo thiết kế B1:
--   - Mỗi bản ghi audit lưu:
--       + user_id      : ai thực hiện
--       + action       : hành động (ORDER_CREATE, USER_UPDATE...)
--       + entity       : tên entity chính (order, invoice, user, ...)
--       + entity_id    : id của entity chính
--       + before_data  : dữ liệu trước khi thay đổi (JSON)
--       + after_data   : dữ liệu sau khi thay đổi (JSON)
--       + created_at   : thời điểm thực hiện
--
-- Lưu ý:
--   - Không được log mật khẩu hoặc dữ liệu cực kỳ nhạy cảm.
--   - Dùng JSONB để dễ query / phân tích sau này.
-- =====================================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id           BIGSERIAL PRIMARY KEY,              -- Khóa chính tự tăng

    user_id      BIGINT,                             -- ID user thực hiện hành động (FK tới app_user.id)
    action       VARCHAR(100)   NOT NULL,            -- Mã hành động, chuẩn UPPER_SNAKE_CASE (VD: ORDER_CREATE, USER_LOGIN)
    entity       VARCHAR(100)   NOT NULL,            -- Tên entity chính: "user", "order", "invoice", "ingredient", ...
    entity_id    BIGINT,                             -- ID của entity chính (có thể null với 1 số action không gắn entity cụ thể)

    before_data  JSONB,                              -- Dữ liệu trước khi thay đổi (CREATE: null)
    after_data   JSONB,                              -- Dữ liệu sau khi thay đổi (DELETE: null)

    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP  -- Thời gian tạo log
);

-- =====================================================================
-- Ràng buộc khóa ngoại (nếu bảng app_user đã tồn tại)
--   - Không dùng ON DELETE CASCADE để tránh mất log khi xóa user
-- =====================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_name = 'app_user'
    ) THEN
        ALTER TABLE audit_log
        ADD CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES app_user(id);
    END IF;
END $$;

-- =====================================================================
-- Index hỗ trợ query nhanh:
--   - Theo user_id         : xem lịch sử thao tác của 1 user
--   - Theo entity + id     : xem lịch sử thay đổi của 1 bản ghi
--   - Theo created_at DESC : xem log mới nhất
-- =====================================================================

-- Index theo user_id
CREATE INDEX IF NOT EXISTS idx_audit_log_user
    ON audit_log (user_id);

-- Index theo entity + entity_id
CREATE INDEX IF NOT EXISTS idx_audit_log_entity
    ON audit_log (entity, entity_id);

-- Index theo created_at (phục vụ sort mới nhất)
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at
    ON audit_log (created_at DESC);

-- =====================================================================
-- KẾT THÚC MIGRATION V16 – BẢNG audit_log
-- =====================================================================
