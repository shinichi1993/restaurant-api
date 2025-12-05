package com.restaurant.api.entity;

import com.restaurant.api.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity Notification
 * -------------------------------------------------------------------
 * Đại diện cho bảng notification trong DB.
 *
 * Ý nghĩa:
 *  - Lưu thông tin 1 thông báo dùng chung cho nhiều user
 *    (VD: "Order #101 vừa được tạo", "Nguyên liệu X sắp hết"...)
 *
 * Map với bảng:
 *  - Bảng: notification
 *  - Cột :
 *      + id         : BIGSERIAL
 *      + title      : VARCHAR(255)
 *      + message    : TEXT
 *      + type       : VARCHAR(50)
 *      + link       : VARCHAR(255)
 *      + created_at : TIMESTAMP
 *
 * Quan hệ:
 *  - 1 Notification có thể thuộc nhiều NotificationUserStatus
 *    (mỗi user 1 trạng thái đọc khác nhau)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    /**
     * Tiêu đề ngắn gọn của thông báo.
     * VD: "Order mới", "Stock sắp hết"...
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Nội dung chi tiết của thông báo.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Loại thông báo:
     *  - ORDER  : liên quan tới đơn hàng
     *  - STOCK  : liên quan tới kho / nguyên liệu
     *  - PAYMENT: liên quan thanh toán
     *  - SYSTEM : thông báo hệ thống
     *  - OTHER  : loại khác
     *
     * Lưu ở DB dạng VARCHAR, map với enum NotificationType.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * Link điều hướng FE:
     *  - VD: "/orders/123", "/stock-entries", "/payments"
     *  - FE dùng để khi click vào thông báo sẽ điều hướng tới màn phù hợp.
     */
    @Column(length = 255)
    private String link;

    /**
     * Thời điểm tạo thông báo.
     * Khớp với cột created_at TIMESTAMP trong DB.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Danh sách trạng thái thông báo theo từng user.
     * mappedBy = "notification" ở entity NotificationUserStatus.
     */
    @OneToMany(mappedBy = "notification", fetch = FetchType.LAZY)
    private List<NotificationUserStatus> userStatuses;
}
