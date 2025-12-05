package com.restaurant.api.dto.dish;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DishRequest – DTO dùng cho API tạo / cập nhật món ăn.
 * -------------------------------------------------------------
 * Dùng trong:
 *  - POST /api/dishes        (tạo mới)
 *  - PUT  /api/dishes/{id}   (cập nhật)
 * -------------------------------------------------------------
 * Quy tắc:
 *  - name:      bắt buộc, không được để trống
 *  - categoryId: bắt buộc, phải là ID category hợp lệ
 *  - price:     bắt buộc, > 0 (BigDecimal theo Rule 26)
 *  - status:    bắt buộc, ACTIVE / INACTIVE
 *  - imageUrl:  có thể null (sau này FE sẽ dùng để hiển thị ảnh)
 * -------------------------------------------------------------
 */
@Data
public class DishRequest {

    /**
     * Tên món ăn
     * Ví dụ: "Bánh đa cua", "Canh cá Quỳnh Côi"
     */
    @NotBlank(message = "Tên món ăn không được để trống")
    private String name;

    /**
     * ID danh mục (Category) mà món ăn thuộc về.
     * Ví dụ: 1 = Món nước, 2 = Đồ uống, ...
     */
    @NotNull(message = "Danh mục món ăn không được để trống")
    private Long categoryId;

    /**
     * Giá bán của món ăn.
     * Dùng BigDecimal để đảm bảo độ chính xác theo Rule 26.
     * Yêu cầu: > 0
     */
    @NotNull(message = "Giá bán không được để trống")
    @Min(value = 1, message = "Giá bán phải lớn hơn 0")
    private BigDecimal price;

    /**
     * Đường dẫn ảnh món ăn (URL).
     * Có thể để trống nếu chưa có ảnh.
     * Ví dụ: "https://example.com/images/banh-da-cua.jpg"
     */
    private String imageUrl;

    /**
     * Trạng thái của món ăn:
     *  - ACTIVE: đang bán
     *  - INACTIVE: tạm ngừng bán
     */
    @NotBlank(message = "Trạng thái món ăn không được để trống")
    private String status;
}
