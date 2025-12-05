package com.restaurant.api.repository;

import com.restaurant.api.entity.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StockEntryRepository
 * --------------------------------------------
 * Repository thao tác với bảng stock_entry.
 * - Cung cấp CRUD cơ bản.
 * - Thêm hàm tìm theo khoảng thời gian để lọc lịch sử.
 */
public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {

    /**
     * Lấy danh sách phiếu trong khoảng thời gian tạo.
     */
    List<StockEntry> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    /**
     * Lấy toàn bộ phiếu, order theo thời gian mới nhất.
     */
    List<StockEntry> findAllByOrderByCreatedAtDesc();
}
