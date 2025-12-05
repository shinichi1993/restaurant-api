package com.restaurant.api.dto.notification;

import com.restaurant.api.enums.NotificationStatus;
import com.restaurant.api.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * NotificationResponse
 * -------------------------------------------------------------------
 * DTO trả ra FE mỗi khi load danh sách thông báo.
 *
 * FE dùng để hiển thị:
 *  - Tiêu đề
 *  - Nội dung
 *  - Loại thông báo (màu sắc khác nhau)
 *  - Đường dẫn chuyển trang (link)
 *  - Trạng thái đã đọc / chưa đọc
 *
 * Lưu ý:
 *  - Đây KHÔNG phải entity, chỉ dùng để trả JSON cho FE.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;                    // ID thông báo gốc
    private String title;               // Tiêu đề ngắn
    private String message;             // Nội dung chi tiết
    private NotificationType type;      // ORDER / STOCK / PAYMENT / SYSTEM
    private String link;                // Link điều hướng FE
    private NotificationStatus status;  // READ / UNREAD
    private LocalDateTime createdAt;    // Thời điểm tạo thông báo

    private LocalDateTime readAt;       // Thời điểm user đọc (nếu có)
}
