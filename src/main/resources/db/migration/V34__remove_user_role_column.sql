-- Vxx__remove_user_role_column.sql
-- -------------------------------------------------------------------
-- Module 4.1: Option A - bỏ cột role enum trên app_user (nếu đang tồn tại)
-- Lưu ý: dùng IF EXISTS để chạy an toàn nhiều môi trường
-- -------------------------------------------------------------------

ALTER TABLE app_user
DROP COLUMN IF EXISTS role;
