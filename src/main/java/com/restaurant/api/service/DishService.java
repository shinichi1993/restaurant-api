package com.restaurant.api.service;

import com.restaurant.api.dto.dish.DishRequest;
import com.restaurant.api.dto.dish.DishResponse;
import com.restaurant.api.entity.Category;
import com.restaurant.api.entity.Dish;
import com.restaurant.api.repository.CategoryRepository;
import com.restaurant.api.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DishService – Xử lý toàn bộ nghiệp vụ liên quan đến MÓN ĂN.
 * ------------------------------------------------------------------
 * Chức năng chính:
 *  - Lấy danh sách tất cả món ăn
 *  - Lấy danh sách món theo Category
 *  - Tạo mới món ăn
 *  - Cập nhật món ăn
 *  - Xóa mềm món ăn (status → INACTIVE)
 *
 * Quy tắc nghiệp vụ:
 *  - Mỗi món phải thuộc về một Category hợp lệ
 *  - Tên món KHÔNG được trùng trong cùng một Category
 *  - Giá bán phải > 0 (BigDecimal)
 *  - Trạng thái chỉ chấp nhận: ACTIVE / INACTIVE
 *
 * Áp dụng:
 *  - Rule 13: comment tiếng Việt đầy đủ
 *  - Rule 26: dùng BigDecimal cho giá, DTO chuẩn
 *  - Rule 28: chuẩn hoá BE (service tách riêng, không truy vấn trực tiếp trong controller)
 * ------------------------------------------------------------------
 */
@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;

    // ==========================================================
    // 1. LẤY DANH SÁCH TẤT CẢ MÓN ĂN
    // ==========================================================

    /**
     * Lấy toàn bộ món ăn trong hệ thống.
     * Dùng cho:
     *  - Trang quản lý món ăn
     *  - Các module khác muốn hiển thị danh sách món (VD: chọn món trong Order)
     */
    public List<DishResponse> getAll() {
        return dishRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==========================================================
    // 2. LẤY DANH SÁCH MÓN THEO CATEGORY
    // ==========================================================

    /**
     * Lấy danh sách món theo Category.
     * Dùng cho:
     *  - Filter trên FE: lọc món theo danh mục
     *  - Module Recipe (định lượng): chọn món theo danh mục
     *
     * @param categoryId ID danh mục
     */
    public List<DishResponse> getByCategory(Long categoryId) {
        // 1. Tìm Category – nếu không tồn tại thì báo lỗi
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // 2. Lấy danh sách Dish theo Category
        return dishRepository.findByCategory(category)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==========================================================
    // 3. TẠO MỚI MÓN ĂN
    // ==========================================================

    /**
     * Tạo mới một món ăn.
     * Đảm bảo:
     *  - Category phải tồn tại
     *  - Không trùng tên món trong cùng Category
     *  - Giá bán > 0
     *  - Trạng thái hợp lệ
     */
    public DishResponse create(DishRequest req) {

        // 1. Lấy Category từ DB
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // 2. Kiểm tra trùng tên trong cùng Category
        dishRepository.findByNameAndCategory(req.getName(), category)
                .ifPresent(d -> {
                    throw new RuntimeException("Tên món ăn đã tồn tại trong danh mục này");
                });

        // 3. Validate giá bán > 0 (double-check ngoài annotation)
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá bán phải lớn hơn 0");
        }

        // 4. Validate trạng thái
        validateStatus(req.getStatus());

        // 5. Tạo entity Dish
        Dish dish = Dish.builder()
                .name(req.getName())
                .category(category)
                .price(req.getPrice())
                .imageUrl(req.getImageUrl())
                .status(req.getStatus())
                .build();

        // 6. Lưu DB
        dishRepository.save(dish);

        // 7. Trả về DTO Response
        return toResponse(dish);
    }

    // ==========================================================
    // 4. CẬP NHẬT MÓN ĂN
    // ==========================================================

    /**
     * Cập nhật thông tin món ăn.
     * Các nghiệp vụ:
     *  - Có thể đổi tên món
     *  - Có thể đổi category
     *  - Có thể đổi giá bán
     *  - Có thể đổi trạng thái
     *  - Không cho phép trùng tên trong cùng Category
     */
    public DishResponse update(Long id, DishRequest req) {

        // 1. Tìm món ăn theo ID
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        // 2. Lấy Category mới từ DB
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // 3. Kiểm tra trùng tên trong cùng Category:
        //    - Tìm món có cùng name + category
        //    - Nếu tồn tại và ID khác ID hiện tại → trùng
        dishRepository.findByNameAndCategory(req.getName(), category)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Tên món ăn đã tồn tại trong danh mục này");
                    }
                });

        // 4. Validate giá bán > 0
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá bán phải lớn hơn 0");
        }

        // 5. Validate trạng thái
        validateStatus(req.getStatus());

        // 6. Cập nhật dữ liệu
        dish.setName(req.getName());
        dish.setCategory(category);
        dish.setPrice(req.getPrice());
        dish.setImageUrl(req.getImageUrl());
        dish.setStatus(req.getStatus());

        // 7. Lưu DB
        dishRepository.save(dish);

        // 8. Trả về DTO Response
        return toResponse(dish);
    }

    // ==========================================================
    // 5. XÓA MÓN ĂN (XÓA MỀM)
    // ==========================================================

    /**
     * Xóa món ăn (xóa mềm bằng cách đổi trạng thái thành INACTIVE).
     * Lý do:
     *  - Đảm bảo lịch sử Order / Invoice không bị mất tham chiếu
     */
    public void delete(Long id) {

        // 1. Tìm món ăn
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        // 2. Xóa mềm: chuyển trạng thái sang INACTIVE
        dish.setStatus("INACTIVE");

        // 3. Lưu DB
        dishRepository.save(dish);
    }

    // ==========================================================
    // 6. HÀM PHỤ: VALIDATE TRẠNG THÁI
    // ==========================================================

    /**
     * Kiểm tra trạng thái món ăn có hợp lệ không.
     * Chỉ chấp nhận:
     *  - ACTIVE
     *  - INACTIVE
     */
    private void validateStatus(String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new RuntimeException("Trạng thái món ăn không hợp lệ (chỉ cho phép ACTIVE / INACTIVE)");
        }
    }

    // ==========================================================
    // 7. HÀM PHỤ: CHUYỂN ENTITY → DTO
    // ==========================================================

    /**
     * Ánh xạ từ Entity Dish → DTO DishResponse.
     * Dùng cho tất cả API trả dữ liệu món ăn ra ngoài.
     */
    private DishResponse toResponse(Dish dish) {
        return DishResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .categoryId(
                        dish.getCategory() != null ? dish.getCategory().getId() : null
                )
                .categoryName(
                        dish.getCategory() != null ? dish.getCategory().getName() : null
                )
                .price(dish.getPrice())
                .imageUrl(dish.getImageUrl())
                .status(dish.getStatus())
                .createdAt(dish.getCreatedAt())
                .updatedAt(dish.getUpdatedAt())
                .build();
    }
}
