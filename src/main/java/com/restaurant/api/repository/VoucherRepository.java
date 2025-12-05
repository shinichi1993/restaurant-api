package com.restaurant.api.repository;

import com.restaurant.api.entity.Voucher;
import com.restaurant.api.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * VoucherRepository – Tầng truy cập dữ liệu cho Voucher
 * ---------------------------------------------------------
 * - Cung cấp các hàm CRUD cơ bản từ JpaRepository
 * - Bổ sung hàm findByCodeAndStatus để phục vụ apply voucher
 */
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    /**
     * Tìm voucher theo mã và trạng thái
     * - Dùng cho luồng áp dụng voucher:
     *   + Chỉ áp dụng khi voucher đang ACTIVE
     */
    Optional<Voucher> findByCodeAndStatus(String code, VoucherStatus status);

    /**
     * Tìm voucher chỉ theo code (phục vụ các xử lý khác nếu cần)
     */
    Optional<Voucher> findByCode(String code);
}
