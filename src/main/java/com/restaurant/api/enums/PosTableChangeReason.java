package com.restaurant.api.enums;

/**
 * PosTableChangeReason
 * ============================================================
 * Enum mô tả NGUYÊN NHÂN khiến trạng thái bàn thay đổi.
 *
 * Dùng cho:
 *  - Realtime POS Table (Phase 5.3.5)
 *
 * LƯU Ý:
 *  - KHÔNG phải trạng thái bàn
 *  - Chỉ là lý do phát sinh event
 */
public enum PosTableChangeReason {

    /**
     * Tạo order mới cho bàn
     * AVAILABLE → OCCUPIED
     */
    ORDER_CREATED,

    /**
     * Thanh toán xong order
     * OCCUPIED → AVAILABLE
     */
    PAYMENT_DONE,

    /**
     * Trạng thái món thay đổi (dành cho Phase 5.3.6+)
     */
    ITEM_STATUS_CHANGED,

    /**
     * Gộp / tách / chuyển bàn
     */
    TABLE_STRUCTURE_CHANGED
}
