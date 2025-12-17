package com.restaurant.api.dto.backup;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermissionDto {
    private Long id;
    private Long roleId;
    private Long permissionId;
}
