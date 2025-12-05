package com.restaurant.api.enums;

/**
 * TableStatus – Enum trạng thái của bàn trong nhà hàng.
 * ------------------------------------------------------
 * AVAILABLE: Bàn trống, có thể nhận khách mới
 * OCCUPIED: Bàn đang có khách (đang có order mở)
 * RESERVED: Bàn đã được đặt trước
 * MERGED:   Bàn đã được gộp vào bàn khác (không dùng trực tiếp)
 */
public enum TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    MERGED
}
