INSERT INTO permission (code, name, description)
VALUES
-- MEMBER
('MEMBER_VIEW', 'Xem hội viên', 'Cho phép xem danh sách hội viên'),
('MEMBER_CREATE', 'Tạo hội viên', 'Cho phép tạo hội viên mới'),
('MEMBER_UPDATE', 'Sửa hội viên', 'Cho phép chỉnh sửa thông tin hội viên'),
('MEMBER_DELETE', 'Xóa hội viên', 'Cho phép xóa hội viên'),

-- RECIPE
('RECIPE_CREATE', 'Tạo định lượng', 'Cho phép tạo định lượng mới'),
('RECIPE_DELETE', 'Xóa định lượng', 'Cho phép xóa định lượng'),

-- TABLE
('TABLE_VIEW', 'Xem tất cả bàn', 'Cho phép xem danh sách bàn'),
('TABLE_CREATE', 'Tạo bàn mới', 'Cho phép tạo bàn mới'),
('TABLE_UPDATE', 'Sửa bàn', 'Cho phép chỉnh sửa thông tin bàn ăn'),
('TABLE_DELETE', 'Xóa bàn', 'Cho phép xóa bàn ăn'),

-- AUDIT
('AUDIT_VIEW', 'Xem log hệ thống', 'Cho phép xem danh sách kiểm tra log'),
('AUDIT_CREATE', 'Tạo log', 'Cho phép tạo log mới'),
('AUDIT_UPDATE', 'Sửa log', 'Cho phép chỉnh sửa thông tin log'),
('AUDIT_DELETE', 'Xóa log', 'Cho phép xóa log');