package com.restaurant.api.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DateTimeUtil
 * ------------------------------------------------------------------
 * Tiện ích xử lý ngày giờ theo chuẩn toàn hệ thống.
 *
 * Chuẩn Rule 26:
 *   - Format: dd/MM/yyyy HH:mm
 *
 * Dùng cho:
 *   - Audit Log
 *   - Report
 *   - Filter theo ngày
 * ------------------------------------------------------------------
 */
public class DateTimeUtil {

    // Formatter chuẩn Rule 26
    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Parse String → LocalDateTime theo Rule 26.
     *
     * @param value chuỗi ngày giờ (dd/MM/yyyy HH:mm)
     * @return LocalDateTime hoặc null nếu value null / rỗng
     */
    public static LocalDateTime parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
    }

    /**
     * Format LocalDateTime → String theo Rule 26.
     * (Dùng khi cần trả ra FE)
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }
}
