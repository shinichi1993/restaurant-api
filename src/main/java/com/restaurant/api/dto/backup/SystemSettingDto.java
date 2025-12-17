package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.SettingValueType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemSettingDto {
    private Long id;
    private String settingGroup;
    private String settingKey;
    private String settingValue;
    private SettingValueType valueType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
