package com.restaurant.api.service;

import com.restaurant.api.dto.orderslip.OrderSlipExportData;
import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.OrderItem;
import com.restaurant.api.export.orderslip.OrderSlipPdfExporterA5;
import com.restaurant.api.export.orderslip.OrderSlipPdfExporterThermal;
import com.restaurant.api.repository.OrderItemRepository;
import com.restaurant.api.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderSlipExportService
 * ------------------------------------------------------------
 * Service xuất PHIẾU GỌI MÓN (Order Slip)
 */
@Service
@RequiredArgsConstructor
public class OrderSlipExportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SystemSettingService systemSettingService;

    private final OrderSlipPdfExporterA5 a5Exporter;
    private final OrderSlipPdfExporterThermal thermalExporter;

    /**
     * Export Order Slip theo layout cấu hình.
     */
    @Transactional(readOnly = true)
    public byte[] exportOrderSlip(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy order"));

        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);

        OrderSlipExportData data = buildExportData(order, items);

        // Đọc cấu hình layout
        // VD: ORDER_SLIP_PRINT_LAYOUT = A5 | THERMAL
        String layout = systemSettingService.getStringSetting(
                "order_slip.print_layout",
                "THERMAL"
        );

        if ("A5".equalsIgnoreCase(layout)) {
            return a5Exporter.export(data);
        }
        return thermalExporter.export(data);
    }

    /**
     * Build DTO export từ entity.
     */
    private OrderSlipExportData buildExportData(Order order, List<OrderItem> items) {

        return OrderSlipExportData.builder()
                .restaurantName(systemSettingService.getStringSetting("restaurant.name", "CỬA HÀNG"))
                .restaurantAddress(systemSettingService.getStringSetting("restaurant.address", ""))
                .restaurantPhone(systemSettingService.getStringSetting("restaurant.phone", ""))
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .tableName(order.getTable() != null ? order.getTable().getName() : null)
                .createdAt(order.getCreatedAt())
                .note(order.getNote())
                .items(
                        items.stream()
                                .map(i -> OrderSlipExportData.Item.builder()
                                        .dishName(i.getDish().getName())
                                        .quantity(i.getQuantity())
                                        .dishPrice(i.getSnapshotPrice())
                                        .subtotal(
                                                i.getSnapshotPrice()
                                                        .multiply(BigDecimal.valueOf(i.getQuantity()))
                                        )
                                        .note(i.getNote())
                                        .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }
}
