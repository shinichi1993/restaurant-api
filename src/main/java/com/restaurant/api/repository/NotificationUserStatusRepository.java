package com.restaurant.api.repository;

import com.restaurant.api.entity.Notification;
import com.restaurant.api.entity.NotificationUserStatus;
import com.restaurant.api.entity.User;
import com.restaurant.api.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * NotificationUserStatusRepository
 * -------------------------------------------------------------------
 * Repository thao tác với bảng notification_user_status.
 *
 * Mục đích:
 *  - Lấy danh sách thông báo theo user + trạng thái
 *  - Đếm số thông báo chưa đọc
 *  - Tìm 1 bản ghi cụ thể (notification, user)
 */
public interface NotificationUserStatusRepository extends JpaRepository<NotificationUserStatus, Long> {

    /**
     * Lấy tất cả trạng thái thông báo của 1 user,
     * sắp xếp mới nhất lên đầu.
     */
    List<NotificationUserStatus> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Lấy danh sách thông báo theo user + trạng thái (UNREAD / READ).
     */
    List<NotificationUserStatus> findByUserAndStatusOrderByCreatedAtDesc(
            User user,
            NotificationStatus status
    );

    /**
     * Đếm số thông báo chưa đọc của 1 user.
     * Dùng cho icon badge ở FE (số đỏ).
     */
    long countByUserAndStatus(User user, NotificationStatus status);

    /**
     * Tìm 1 bản ghi cụ thể theo (notification, user).
     * Dùng để cập nhật trạng thái READ khi user bấm xem.
     */
    Optional<NotificationUserStatus> findByNotificationAndUser(
            Notification notification,
            User user
    );
}
