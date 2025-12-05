-- V12__create_role_permission_tables.sql
-- ============================================================================
-- Tạo các bảng phục vụ phân quyền (Module 13 – Roles & Permission)
-- Gồm:
--   - role               : Danh sách vai trò trong hệ thống
--   - permission         : Danh sách quyền chi tiết
--   - role_permission    : Bảng trung gian role <-> permission
--   - user_role          : Bảng trung gian user <-> role
-- Lưu ý:
--   - Nếu tên bảng user hiện tại khác (vd: users, app_user) thì sửa lại FK
--     ở phần tạo bảng user_role cho đúng DB thực tế.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Bảng ROLE
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,   -- Mã role (ADMIN, STAFF...)
    name        VARCHAR(100) NOT NULL,          -- Tên hiển thị
    description VARCHAR(255),                   -- Mô tả thêm
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tạo index để tìm kiếm nhanh theo code (phòng trường hợp query nhiều)
CREATE INDEX IF NOT EXISTS idx_role_code ON role (code);

-- ----------------------------------------------------------------------------
-- 2. Bảng PERMISSION
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,   -- Mã quyền (ORDER_VIEW...)
    name        VARCHAR(150) NOT NULL,          -- Tên hiển thị
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_permission_code ON permission (code);

-- ----------------------------------------------------------------------------
-- 3. Bảng ROLE_PERMISSION (quan hệ N-N giữa role và permission)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_permission (
    id             BIGSERIAL PRIMARY KEY,
    role_id        BIGINT NOT NULL,
    permission_id  BIGINT NOT NULL,

    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id) REFERENCES role (id),

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES permission (id),

    -- Không cho phép 1 quyền được gán 2 lần cho cùng 1 role
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permission_role_id
    ON role_permission (role_id);

CREATE INDEX IF NOT EXISTS idx_role_permission_permission_id
    ON role_permission (permission_id);

-- ----------------------------------------------------------------------------
-- 4. Bảng USER_ROLE (quan hệ N-N giữa user và role)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_role (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,

    -- TODO: Sửa lại tên bảng "user" bên dưới cho đúng với bảng user thực tế
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),

    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id) REFERENCES role (id),

    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON user_role (user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON user_role (role_id);
