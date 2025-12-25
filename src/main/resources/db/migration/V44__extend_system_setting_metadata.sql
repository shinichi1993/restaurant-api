-- =====================================================================
-- V44__extend_system_setting_metadata.sql
-- ---------------------------------------------------------------------
-- Mục tiêu:
--  - Mở rộng bảng system_setting để hỗ trợ FE render động 100%
--  - Không cần hard-code key trên FE nữa
--  - Migrate dữ liệu cũ: tự sinh label/input_type mặc định theo value_type
-- =====================================================================

ALTER TABLE system_setting
    ADD COLUMN IF NOT EXISTS label VARCHAR(255),
    ADD COLUMN IF NOT EXISTS input_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS min_value NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS max_value NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS visible BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS editable BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS depends_on_key VARCHAR(150),
    ADD COLUMN IF NOT EXISTS depends_on_value VARCHAR(50);

-- 1) Label mặc định: nếu chưa có label thì dùng setting_key (FE vẫn hiển thị được ngay)
UPDATE system_setting
SET label = setting_key
WHERE label IS NULL OR label = '';

-- 2) Input type mặc định theo value_type:
--    - BOOLEAN -> SWITCH
--    - NUMBER  -> NUMBER
--    - STRING/JSON -> INPUT
UPDATE system_setting
SET input_type = CASE
    WHEN value_type = 'BOOLEAN' THEN 'SWITCH'
    WHEN value_type = 'NUMBER' THEN 'NUMBER'
    ELSE 'INPUT'
END
WHERE input_type IS NULL OR input_type = '';

-- 3) Mặc định visible/editable nếu null
UPDATE system_setting SET visible = TRUE WHERE visible IS NULL;
UPDATE system_setting SET editable = TRUE WHERE editable IS NULL;

-- 4) Thiết lập dependency chuẩn cho 1 case đang hard-code ở FE:
--    pos.simple_pos_require_table chỉ hiển thị khi pos.simple_pos_mode = true
UPDATE system_setting
SET depends_on_key = 'pos.simple_pos_mode',
    depends_on_value = 'true'
WHERE setting_key = 'pos.simple_pos_require_table'
  AND (depends_on_key IS NULL OR depends_on_key = '');

-- 5) Tạo index hỗ trợ sort theo group + order
CREATE INDEX IF NOT EXISTS idx_system_setting_group_order
    ON system_setting(setting_group, order_index);
