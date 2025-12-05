package com.restaurant.api.entity;

import com.restaurant.api.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity NotificationUserStatus
 * -------------------------------------------------------------------
 * Đại diện cho bảng notification_user_status.
 *
 * Ý nghĩa:
 *  - Lưu TRẠNG THÁI của 1 thông báo đối với TỪNG USER.
 *  - Ví dụ:
 *      + Notification id=1 (Order #101 tạo thành công)
 *      + User A: UNREAD
 *      + User B: READ lúc 10:05
 *
 * Bảng:
 *  - notification_user_status
 *  - Có UNIQUE (notification_id, user_id) trong DB
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_user_status")
public class NotificationUserStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    /**
     * Thông báo gốc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    /**
     * User nhận thông báo.
     *
     * ⚠ Lưu ý:
     *  - Nếu entity user của bạn có tên khác (User, AppUser...),
     *    hãy đổi lại cho khớp và sửa import tương ứng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Trạng thái đọc:
     *  - UNREAD : chưa đọc
     *  - READ   : đã đọc
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    /**
     * Thời điểm user đọc thông báo (nếu status = READ).
     * Có thể null nếu chưa đọc.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Thời điểm gán thông báo cho user.
     * Thường trùng với lúc tạo NotificationUserStatus.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
