package com.restaurant.api.export.invoice;

/**
 * InvoicePrintLayout
 * ------------------------------------------------------------------
 * Enum quy ước các kiểu layout in hóa đơn.
 *
 * Hiện tại hỗ trợ:
 *  - A5          : In khổ A5 dọc (InvoicePdfExporterA5)
 *  - THERMAL_80  : In khổ giấy nhiệt 80mm (InvoicePdfExporterThermal)
 *
 * Lưu ý:
 *  - Enum này chỉ dùng nội bộ BE để chọn exporter.
 *  - Việc đọc cấu hình layout từ SystemSetting sẽ làm ở step khác
 *    (ví dụ: key "invoice.print_layout" trả về "A5" hoặc "THERMAL_80").
 */
public enum InvoicePrintLayout {
    A5,
    THERMAL_80;
}
