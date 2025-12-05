package com.restaurant.api.enums;

/**
 * NotificationType
 * -------------------------------------------------------------------
 * Liệt kê các loại thông báo trong hệ thống.
 *
 * Lưu ý:
 *  - Được lưu xuống DB dạng chuỗi (EnumType.STRING)
 *  - Có thể mở rộng thêm nếu cần.
 */
public enum NotificationType {

    /**
     * Thông báo liên quan tới ORDER / đơn hàng.
     * Ví dụ:
     *  - Order mới được tạo
     *  - Order bị hủy, v.v.
     */
    ORDER,

    /**
     * Thông báo liên quan tới kho / nguyên liệu.
     * Ví dụ:
     *  - Nguyên liệu sắp hết
     *  - Nhập kho thất bại, v.v.
     */
    STOCK,

    /**
     * Thông báo liên quan tới thanh toán / hóa đơn.
     */
    PAYMENT,

    /**
     * Thông báo hệ thống chung:
     *  - Cấu hình thay đổi
     *  - Thông báo bảo trì, v.v.
     */
    SYSTEM,

    /**
     * Loại khác, không thuộc các nhóm trên.
     */
    OTHER
}
