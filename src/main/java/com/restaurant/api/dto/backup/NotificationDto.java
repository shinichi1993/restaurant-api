package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private String link;
    private LocalDateTime createdAt;
}
