package com.restaurant.api.dto.category;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO trả dữ liệu Category ra FE.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
