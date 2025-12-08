package com.restaurant.api.enums;

/**
 * OrderItemStatus
 * ----------------------------------------------------
 * Enum trạng thái xử lý của TỪNG MÓN trong 1 order.
 * Dùng cho:
 *  - Màn hình POS: hiển thị trạng thái món (mới tạo / đã gửi bếp / đang nấu / xong / hủy)
 *  - Màn hình Bếp: cập nhật tiến trình chế biến món
 *  - Logic Phase 2 POS Advanced: kiểm soát các thao tác được phép
 *
 * Các trạng thái:
 *  - NEW             : Món mới được thêm vào order, chưa gửi xuống bếp
 *  - SENT_TO_KITCHEN : Đã gửi thông tin món xuống bếp, bếp chưa bấm nhận
 *  - COOKING         : Bếp đã nhận món và đang chế biến
 *  - DONE            : Món đã nấu xong, sẵn sàng phục vụ/đã phục vụ
 *  - CANCELED        : Món đã bị hủy (do khách đổi ý hoặc nhân viên thao tác)
 *
 * Lưu ý:
 *  - Trạng thái này là cho TỪNG MÓN, không phải cho cả order.
 *  - Trạng thái order tổng thể vẫn dùng OrderStatus (NEW / SERVING / PAID / CANCELED).
 */
public enum OrderItemStatus {

    /**
     * NEW
     * ----------------------------------------------------
     * - Mặc định khi tạo order mới (createOrder)
     * - Món chỉ mới nằm trong giỏ hàng/order, chưa gửi xuống bếp
     * - FE có thể hiển thị màu xám, status "Chưa gửi bếp"
     */
    NEW,

    /**
     * SENT_TO_KITCHEN
     * ----------------------------------------------------
     * - Món đã được gửi xuống bếp (theo nút "Gửi bếp" hoặc auto_send_kitchen = true)
     * - Bếp đã thấy món trên màn hình Kitchen nhưng chưa bấm "Nhận làm"
     * - FE có thể hiển thị màu xanh nhạt, trạng thái "Đã gửi bếp"
     */
    SENT_TO_KITCHEN,

    /**
     * COOKING
     * ----------------------------------------------------
     * - Bếp đã bấm "Nhận làm" hoặc "Bắt đầu nấu"
     * - Món đang trong quá trình chế biến
     * - FE có thể hiển thị màu cam, trạng thái "Đang nấu"
     */
    COOKING,

    /**
     * DONE
     * ----------------------------------------------------
     * - Bếp đã bấm "Hoàn thành"
     * - Món đã nấu xong, có thể đã được phục vụ cho khách
     * - FE có thể hiển thị màu xanh lá + icon tick "Hoàn thành"
     */
    DONE,

    /**
     * CANCELED
     * ----------------------------------------------------
     * - Món đã bị hủy:
     *    + Khách đổi ý
     *    + Nhân viên sai món, cần hủy
     * - Tùy cấu hình POS (pos.allow_cancel_item) mà BE cho phép/không cho phép hủy
     * - FE hiển thị màu đỏ, trạng thái "Đã hủy"
     */
    CANCELED
}
