-- Tạo bảng lưu lịch sử trigger notification rule
CREATE TABLE notification_rule_log (
    id BIGSERIAL PRIMARY KEY,

    rule_key VARCHAR(100) NOT NULL UNIQUE,
    last_triggered_at TIMESTAMP NOT NULL,
    last_payload_hash VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
