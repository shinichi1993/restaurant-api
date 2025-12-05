-- ============================================================
-- V18__create_restaurant_table.sql
-- MODULE 16 – TABLE MANAGEMENT (Quản lý bàn)
-- ------------------------------------------------------------
-- Mục đích:
--   - Tạo bảng restaurant_table quản lý danh sách bàn trong nhà hàng
--   - Lưu trạng thái bàn: AVAILABLE / OCCUPIED / RESERVED / MERGED
--   - Hỗ trợ các nghiệp vụ: gộp bàn, tách bàn, chuyển bàn
-- ============================================================

CREATE TABLE restaurant_table (
    id BIGSERIAL PRIMARY KEY,                -- Khóa chính

    name VARCHAR(50) NOT NULL,               -- Tên bàn (VD: B1, B2, Tầng 1 - Bàn 3)
    capacity INT NOT NULL DEFAULT 1,         -- Số lượng khách tối đa

    status VARCHAR(20) NOT NULL,             -- Trạng thái bàn:
                                             -- AVAILABLE / OCCUPIED / RESERVED / MERGED

    merged_root_id BIGINT NULL,              -- Nếu bàn này được gộp vào bàn khác
                                             -- => lưu id của bàn gốc (root)

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),  -- Thời gian tạo
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()   -- Thời gian cập nhật gần nhất
);

-- Ràng buộc danh sách trạng thái hợp lệ
ALTER TABLE restaurant_table
ADD CONSTRAINT ck_restaurant_table_status
CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MERGED'));

-- Tên bàn không được trùng nhau (tùy yêu cầu nghiệp vụ, ở đây quy định duy nhất)
ALTER TABLE restaurant_table
ADD CONSTRAINT uq_restaurant_table_name UNIQUE (name);

-- Quan hệ self-reference cho merged_root_id (gộp bàn vào bàn gốc)
ALTER TABLE restaurant_table
ADD CONSTRAINT fk_restaurant_table_merged_root
FOREIGN KEY (merged_root_id) REFERENCES restaurant_table(id);

-- Index để lọc theo trạng thái bàn
CREATE INDEX idx_restaurant_table_status ON restaurant_table(status);

-- Nếu toàn hệ thống đã có hàm trigger_set_timestamp() dùng chung:
-- Tự động cập nhật updated_at mỗi khi UPDATE
CREATE TRIGGER set_timestamp_restaurant_table
BEFORE UPDATE ON restaurant_table
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();
