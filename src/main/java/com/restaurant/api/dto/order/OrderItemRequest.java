package com.restaurant.api.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * OrderItemRequest
 * ------------------------------------------------------------
 * DTO cho từng món khi tạo order
 * - dishId: ID món ăn
 * - quantity: số lượng gọi
 * ------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    @NotNull(message = "Món ăn không được để trống")
    private Long dishId; // ID món

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity; // Số phần khách gọi
}
