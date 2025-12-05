-- Chuyển cột trước/sau thay đổi từ JSONB → TEXT
-- Vì AuditLogService đang lưu chuỗi JSON string
-- TEXT linh hoạt, không kén kiểu, không cần cast

ALTER TABLE audit_log
    ALTER COLUMN before_data TYPE TEXT,
    ALTER COLUMN after_data TYPE TEXT;