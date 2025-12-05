package com.restaurant.api.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO tạo/sửa Category.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    @NotNull
    private String name;
    private String description;
    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // ACTIVE / INACTIVE
}
