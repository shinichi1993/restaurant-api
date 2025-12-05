INSERT INTO permission (code, name, description)
VALUES
-- USER
('USER_VIEW', 'Xem người dùng', 'Cho phép xem danh sách người dùng'),
('USER_CREATE', 'Tạo người dùng', 'Cho phép tạo tài khoản mới'),
('USER_UPDATE', 'Sửa người dùng', 'Cho phép chỉnh sửa thông tin người dùng'),
('USER_DELETE', 'Xóa người dùng', 'Cho phép xóa người dùng'),

-- CATEGORY & DISH
('CATEGORY_VIEW', 'Xem danh mục', 'Cho phép xem danh mục món ăn'),
('CATEGORY_CREATE', 'Tạo danh mục', 'Cho phép tạo danh mục món ăn'),
('CATEGORY_UPDATE', 'Sửa danh mục', 'Cho phép chỉnh sửa danh mục'),
('CATEGORY_DELETE', 'Xóa danh mục', 'Cho phép xóa danh mục'),
('DISH_VIEW', 'Xem món ăn', 'Cho phép xem danh sách món ăn'),
('DISH_CREATE', 'Thêm món ăn', 'Cho phép thêm món ăn'),
('DISH_UPDATE', 'Sửa món ăn', 'Cho phép chỉnh sửa món ăn'),
('DISH_DELETE', 'Xóa món ăn', 'Cho phép xóa món ăn'),

-- INGREDIENT & STOCK
('INGREDIENT_VIEW', 'Xem nguyên liệu', 'Cho phép xem danh sách nguyên liệu'),
('INGREDIENT_CREATE', 'Tạo nguyên liệu', 'Cho phép thêm nguyên liệu'),
('INGREDIENT_UPDATE', 'Sửa nguyên liệu', 'Cho phép chỉnh sửa nguyên liệu'),
('INGREDIENT_DELETE', 'Xóa nguyên liệu', 'Cho phép xóa nguyên liệu'),
('STOCK_VIEW', 'Xem nhập kho', 'Cho phép xem lịch sử nhập kho'),
('STOCK_CREATE', 'Tạo nhập kho', 'Cho phép tạo phiếu nhập kho'),

-- RECIPE
('RECIPE_VIEW', 'Xem định lượng', 'Cho phép xem định lượng món ăn'),
('RECIPE_UPDATE', 'Cập nhật định lượng', 'Cho phép sửa định lượng'),

-- ORDER
('ORDER_VIEW', 'Xem order', 'Cho phép xem danh sách order'),
('ORDER_CREATE', 'Tạo order', 'Cho phép tạo order'),
('ORDER_UPDATE', 'Cập nhật order', 'Cho phép cập nhật trạng thái order'),
('ORDER_CANCEL', 'Hủy order', 'Cho phép hủy order'),

-- INVOICE & PAYMENT
('INVOICE_VIEW', 'Xem hóa đơn', 'Cho phép xem chi tiết hóa đơn'),
('PAYMENT_VIEW', 'Xem thanh toán', 'Cho phép xem danh sách thanh toán'),

-- REPORT
('REPORT_REVENUE', 'Xem báo cáo doanh thu', 'Cho phép xem báo cáo doanh thu'),
('REPORT_TOP_DISH', 'Xem báo cáo top món', 'Cho phép xem top món bán chạy'),
('REPORT_INGREDIENT', 'Xem báo cáo nguyên liệu', 'Cho phép xem báo cáo nguyên liệu'),

-- SETTINGS
('SETTING_VIEW', 'Xem cấu hình', 'Cho phép xem phần cài đặt hệ thống'),
('SETTING_UPDATE', 'Cập nhật cấu hình', 'Cho phép chỉnh sửa cài đặt'),

-- ROLE & PERMISSION
('ROLE_VIEW', 'Xem vai trò', 'Cho phép xem danh sách vai trò'),
('ROLE_CREATE', 'Tạo vai trò', 'Cho phép tạo vai trò'),
('ROLE_UPDATE', 'Sửa vai trò', 'Cho phép cập nhật vai trò'),
('ROLE_DELETE', 'Xóa vai trò', 'Cho phép xóa vai trò');