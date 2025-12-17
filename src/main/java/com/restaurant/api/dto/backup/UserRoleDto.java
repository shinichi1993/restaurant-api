package com.restaurant.api.dto.backup;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRoleDto {
    private Long id;
    private Long userId;
    private Long roleId;
}
