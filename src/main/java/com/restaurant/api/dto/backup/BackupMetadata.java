package com.restaurant.api.dto.backup;

import lombok.*;

import java.time.LocalDateTime;

/**
 * BackupMetadata – metadata.json trong file backup
 * ------------------------------------------------------------
 * Chỉ dùng để tham chiếu (không migrate DB).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupMetadata {
    private LocalDateTime backupAt;
    private String createdBy;
    private String flywayVersion;
    private String appVersion;
}
