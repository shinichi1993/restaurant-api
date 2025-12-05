package com.restaurant.api.enums;

/**
 * AuditAction – Danh sách hành động hệ thống cần log lại.
 * --------------------------------------------------------------
 * Quy tắc đặt tên:
 *   - Chữ IN HOA + gạch dưới
 *   - Nhóm theo module: USER_, ROLE_, INGREDIENT_, ORDER_, INVOICE_, STOCK_…
 *
 * Mỗi action thể hiện 1 nghiệp vụ quan trọng.
 */
public enum AuditAction {

    // Hệ thống / Authentication
    USER_LOGIN,
    USER_LOGOUT,

    // User
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,

    // Role
    ROLE_CREATE,
    ROLE_UPDATE,
    ROLE_DELETE,

    // Permission
    PERMISSION_ASSIGN,
    PERMISSION_REVOKE,

    // Ingredient
    INGREDIENT_CREATE,
    INGREDIENT_UPDATE,
    INGREDIENT_DELETE,

    // Stock Entry
    STOCK_ENTRY_CREATE,
    STOCK_ENTRY_UPDATE,
    STOCK_ENTRY_DELETE,

    // Order
    ORDER_CREATE,
    ORDER_UPDATE,
    ORDER_CANCEL,

    // Invoice & Payment
    INVOICE_CREATE,
    PAYMENT_CREATE
}
