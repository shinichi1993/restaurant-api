------------------------------------------------------------
-- V15 – Thêm ROLE mặc định & gán quyền
-- ADMIN / MANAGER / STAFF / KITCHEN
------------------------------------------------------------

-- ============================================
-- 1. TẠO ROLE
-- ============================================

INSERT INTO role (code, name, description)
VALUES
('ADMIN', 'Quản trị hệ thống', 'Quyền cao nhất, toàn quyền hệ thống'),
('MANAGER', 'Quản lý', 'Quản lý vận hành chung, không có quyền hệ thống'),
('STAFF', 'Nhân viên', 'Nhân viên bán hàng, thao tác order/thanh toán'),
('KITCHEN', 'Bếp', 'Nhân viên bếp, xem order và chế biến')
ON CONFLICT (code) DO NOTHING;


-- ============================================
-- 2. ADMIN – FULL QUYỀN
-- ============================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r, permission p
WHERE r.code = 'ADMIN';


-- ============================================
-- 3. MANAGER – quyền cấp quản lý
-- (Không cho quyền xóa user, xóa role, update setting)
-- ============================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code NOT IN (
    'USER_DELETE',
    'ROLE_DELETE',
    'SETTING_UPDATE'
)
WHERE r.code = 'MANAGER';


-- ============================================
-- 4. STAFF – quyền vận hành
-- Chỉ được:
-- - Order
-- - Payment
-- - Invoice View
-- - Xem món, xem nguyên liệu
-- - Không được chỉnh sửa cấu hình hoặc người dùng
-- ============================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
    'ORDER_VIEW', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_CANCEL',
    'PAYMENT_VIEW',
    'INVOICE_VIEW',
    'DISH_VIEW',
    'CATEGORY_VIEW',
    'INGREDIENT_VIEW',
    'STOCK_VIEW',
    'REPORT_REVENUE', 'REPORT_TOP_DISH', 'REPORT_INGREDIENT'
)
WHERE r.code = 'STAFF';


-- ============================================
-- 5. KITCHEN – quyền xem order để chế biến
-- ============================================

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN (
    'ORDER_VIEW',
    'DISH_VIEW',
    'RECIPE_VIEW'
)
WHERE r.code = 'KITCHEN';