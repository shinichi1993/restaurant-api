package com.restaurant.api.dto.dish;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DishResponse – DTO trả dữ liệu món ăn về cho FE.
 * -------------------------------------------------------------
 * Dùng trong:
 *  - GET /api/dishes
 *  - GET /api/dishes/by-category/{categoryId}
 *  - Sau khi tạo / cập nhật món (POST/PUT)
 * -------------------------------------------------------------
 * FE sẽ dùng model này để hiển thị:
 *  - Danh sách món
 *  - Giá bán
 *  - Trạng thái
 *  - Ảnh món
 *  - Thời gian tạo / cập nhật
 * -------------------------------------------------------------
 */
@Data
@Builder
public class DishResponse {

    /**
     * ID món ăn (khóa chính)
     */
    private Long id;

    /**
     * Tên món ăn
     */
    private String name;

    /**
     * ID danh mục món ăn
     */
    private Long categoryId;

    /**
     * Tên danh mục món ăn
     * FE dùng để hiển thị trực tiếp, tránh phải gọi thêm API.
     */
    private String categoryName;

    /**
     * Giá bán của món (BigDecimal trên BE, number trên FE).
     */
    private BigDecimal price;

    /**
     * Đường dẫn ảnh món ăn (URL).
     */
    private String imageUrl;

    /**
     * Trạng thái món ăn: ACTIVE / INACTIVE.
     */
    private String status;

    /**
     * Thời gian tạo bản ghi.
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật gần nhất.
     */
    private LocalDateTime updatedAt;
}
