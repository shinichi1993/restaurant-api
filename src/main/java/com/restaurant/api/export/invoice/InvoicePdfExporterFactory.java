package com.restaurant.api.export.invoice;

import com.restaurant.api.dto.invoice.InvoiceExportData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * InvoicePdfExporterFactory
 * =====================================================================
 * Factory chịu trách nhiệm chọn đúng exporter PDF cho hóa đơn
 * dựa trên kiểu layout mong muốn (A5 hoặc Thermal 80mm).
 *
 * Nhiệm vụ:
 *  - Tiêm sẵn InvoicePdfExporterA5 và InvoicePdfExporterThermal
 *  - Cung cấp hàm export(layout, data) để:
 *      + Dùng chung cho Controller / Service
 *      + Tránh việc chỗ nào cũng phải if/else theo layout
 *
 * Lưu ý:
 *  - Factory KHÔNG tự đọc SystemSetting.
 *    Việc đọc key "invoice.print_layout" → map sang InvoicePrintLayout
 *    sẽ xử lý ở step khác (trong Service hoặc Controller).
 * =====================================================================
 */
@Component
@RequiredArgsConstructor
public class InvoicePdfExporterFactory {

    private final InvoicePdfExporterA5 a5Exporter;
    private final InvoicePdfExporterThermal thermalExporter;

    /**
     * Export hóa đơn ra PDF theo layout được truyền vào.
     * --------------------------------------------------------------
     * @param layout kiểu layout (A5 hoặc THERMAL_80)
     * @param data   dữ liệu hóa đơn đã chuẩn hóa
     * @return byte[] nội dung file PDF
     */
    public byte[] export(InvoicePrintLayout layout, InvoiceExportData data) {
        if (layout == null) {
            // Nếu null thì mặc định in A5
            layout = InvoicePrintLayout.A5;
        }

        switch (layout) {
            case THERMAL_80:
                return thermalExporter.export(data);
            case A5:
            default:
                return a5Exporter.export(data);
        }
    }
}
