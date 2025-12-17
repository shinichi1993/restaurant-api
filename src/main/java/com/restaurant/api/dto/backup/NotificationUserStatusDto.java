package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.NotificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationUserStatusDto {
    private Long id;
    private Long notificationId;
    private Long userId;
    private NotificationStatus status;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
