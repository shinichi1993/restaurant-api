package com.restaurant.api.service;

import com.restaurant.api.dto.category.*;
import com.restaurant.api.entity.Category;
import com.restaurant.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ Category
 * - Lấy danh sách
 * - Tạo mới
 * - Cập nhật
 * - Xóa mềm
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy toàn bộ danh mục
     */
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo danh mục mới
     */
    public CategoryResponse create(CategoryRequest req) {

        // Kiểm tra trùng tên
        if (categoryRepository.findByName(req.getName()).isPresent()) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }

        Category c = Category.builder()
                .name(req.getName())
                .description(req.getDescription())
                .status(req.getStatus())
                .build();

        categoryRepository.save(c);
        return toResponse(c);
    }

    /**
     * Cập nhật danh mục
     */
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        c.setName(req.getName());
        c.setDescription(req.getDescription());
        c.setStatus(req.getStatus());

        categoryRepository.save(c);
        return toResponse(c);
    }

    /**
     * Xóa mềm danh mục
     */
    public void delete(Long id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        c.setStatus("INACTIVE");
        categoryRepository.save(c);
    }

    /**
     * Convert Entity → DTO
     */
    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
