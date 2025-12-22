package com.restaurant.api.export.orderslip;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderSlipPdfExporterFactory {

    private final OrderSlipPdfExporterA5 a5;
    private final OrderSlipPdfExporterThermal thermal;

    public Object getExporter(String layout) {
        if ("THERMAL".equalsIgnoreCase(layout)) {
            return thermal;
        }
        return a5;
    }
}
