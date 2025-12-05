package com.restaurant.api.repository;

import com.restaurant.api.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * InvoiceItemRepository
 * ------------------------------------------------------------
 * Repository làm việc với bảng invoice_item.
 *
 * Chức năng chính:
 *  - Lưu danh sách chi tiết món của 1 hóa đơn
 *  - Lấy toàn bộ invoice_item theo invoiceId để hiển thị chi tiết
 *
 * Dùng trong:
 *  - InvoiceService: khi map Invoice → InvoiceResponse (lấy danh sách items)
 * ------------------------------------------------------------
 */
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /**
     * Lấy toàn bộ chi tiết món (invoice_item) theo invoiceId.
     */
    List<InvoiceItem> findByInvoiceId(Long invoiceId);
}
