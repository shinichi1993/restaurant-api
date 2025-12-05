package com.restaurant.api.repository;

import com.restaurant.api.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * InvoiceRepository
 * ------------------------------------------------------------
 * Repository làm việc với bảng invoice.
 *
 * Chức năng chính:
 *  - Lưu / cập nhật / xóa Invoice (các hàm có sẵn từ JpaRepository)
 *  - Tìm invoice theo orderId (vì mỗi order chỉ có tối đa 1 invoice)
 *
 * Ghi chú:
 *  - Sử dụng Optional để tránh NullPointerException khi không tìm thấy.
 *  - Sẽ được dùng trong InvoiceService và sau này trong PaymentService.
 * ------------------------------------------------------------
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Tìm hóa đơn theo orderId.
     * Dùng khi cần lấy invoice của 1 đơn hàng cụ thể.
     */
    Optional<Invoice> findByOrderId(Long orderId);
}
