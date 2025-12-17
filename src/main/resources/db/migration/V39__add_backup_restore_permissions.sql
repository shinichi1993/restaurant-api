-- Thêm permission cho Phase 4.4 – Backup/Restore
INSERT INTO permission(code, name, description, created_at, updated_at)
VALUES
('ADMIN_BACKUP', 'Backup dữ liệu', 'Cho phép tạo file backup dữ liệu hệ thống', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO permission(code, name, description, created_at, updated_at)
VALUES
('ADMIN_RESTORE', 'Restore dữ liệu', 'Cho phép restore dữ liệu từ file backup', NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- Gán quyền cho role ADMIN (nếu m dùng code role = 'ADMIN')
INSERT INTO role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.code = 'ADMIN' AND p.code IN ('ADMIN_BACKUP','ADMIN_RESTORE')
ON CONFLICT DO NOTHING;
