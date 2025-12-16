-- V36__add_ip_user_agent_to_audit_log.sql
-- Thêm IP và User-Agent cho audit log (nullable, không phá dữ liệu cũ)

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(50);

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS user_agent TEXT;
