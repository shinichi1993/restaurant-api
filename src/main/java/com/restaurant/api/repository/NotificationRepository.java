package com.restaurant.api.repository;

import com.restaurant.api.entity.Notification;
import com.restaurant.api.enums.NotificationStatus;
import com.restaurant.api.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationRepository
 * -------------------------------------------------------------------
 * Repository thao tác với bảng notification.
 *
 * Các hàm cơ bản:
 *  - findAllByOrderByCreatedAtDesc()
 *  - findByTypeOrderByCreatedAtDesc(...)
 *  - Tìm theo khoảng thời gian nếu cần.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy tất cả thông báo, sắp xếp mới nhất lên đầu.
     */
    List<Notification> findAllByOrderByCreatedAtDesc();

    /**
     * Lấy thông báo theo type (ORDER, STOCK...) sắp xếp mới nhất trước.
     */
    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    /**
     * Lấy thông báo trong khoảng thời gian (nếu sau này cần lọc theo ngày).
     */
    List<Notification> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Kiểm tra đã tồn tại thông báo có title trong khoảng thời gian gần đây chưa.
     * Dùng để chống spam (dedup).
     */
    boolean existsByTitleAndCreatedAtAfter(String title, java.time.LocalDateTime createdAt);
}
