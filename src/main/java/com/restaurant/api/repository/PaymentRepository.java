package com.restaurant.api.repository;

import com.restaurant.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PaymentRepository
 * --------------------------------------------------------------------
 * Repository làm việc với bảng payment.
 *
 * Chức năng chính:
 *  - Lưu / cập nhật / xóa payment (kế thừa từ JpaRepository)
 *  - Tìm payment theo orderId
 *  - Tìm payment theo invoiceId
 *  - Lọc payment theo khoảng thời gian paidAt (dùng cho màn PaymentPage)
 *
 * Ghi chú:
 *  - Không viết logic nghiệp vụ tại đây, chỉ định nghĩa method truy vấn.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Tìm payment theo orderId.
     * Mỗi order trong phiên bản v1 chỉ có 1 payment.
     */
    Optional<Payment> findByOrder_Id(Long orderId);

    /**
     * Tìm payment theo invoiceId.
     * Dùng khi muốn truy ngược từ hóa đơn về payment.
     */
    Optional<Payment> findByInvoice_Id(Long invoiceId);

    /**
     * Lấy danh sách payment theo khoảng thời gian thanh toán.
     * Dùng cho filter trong màn danh sách thanh toán.
     */
    List<Payment> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Tìm payment theo momoOrderId (dùng cho IPN MoMo).
     * Bắt buộc cho online payment (idempotent).
     */
    Optional<Payment> findByMomoOrderId(String momoOrderId);
}
