package com.restaurant.api.dto.notification;

import com.restaurant.api.enums.NotificationType;
import lombok.*;

/**
 * CreateNotificationRequest
 * -------------------------------------------------------------------
 * DTO được sử dụng khi hệ thống cần tạo thông báo mới.
 *
 * Ví dụ:
 *  - Khi Order được tạo: gọi NotificationService.createForUsers(...)
 *  - Khi nguyên liệu sắp hết
 *  - Khi thanh toán thành công
 *
 * Lưu ý:
 *  - Module này dùng nội bộ BE, FE không gọi trực tiếp.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    private String title;           // Tiêu đề
    private String message;         // Nội dung
    private NotificationType type;  // Loại thông báo
    private String link;            // Đường dẫn điều hướng FE

    /**
     * Danh sách user nhận thông báo.
     * Nếu null → thông báo gửi cho toàn bộ user trong hệ thống.
     */
    private java.util.List<Long> userIds;
}
