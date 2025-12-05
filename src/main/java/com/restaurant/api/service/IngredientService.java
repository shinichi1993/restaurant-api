package com.restaurant.api.service;

import com.restaurant.api.dto.ingredient.*;
import com.restaurant.api.entity.Ingredient;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * IngredientService
 * ----------------------------------------------------------
 * Xử lý toàn bộ nghiệp vụ liên quan đến nguyên liệu:
 *  - Lấy danh sách nguyên liệu
 *  - Tạo nguyên liệu mới
 *  - Cập nhật thông tin nguyên liệu
 *  - Xóa nguyên liệu (xóa mềm bằng active = false)
 *  - Chuyển đổi Entity → DTO IngredientResponse
 * ----------------------------------------------------------
 * Tất cả comment được viết bằng tiếng Việt theo Rule 13.
 */
@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final AuditLogService auditLogService;

    /**
     * Lấy toàn bộ nguyên liệu trong hệ thống.
     * Không trả về createdAt/updatedAt → FE không dùng.
     */
    public List<IngredientResponse> getAll() {
        return ingredientRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo mới 1 nguyên liệu.
     * - Mặc định active = true
     */
    public IngredientResponse create(IngredientCreateRequest req) {

        Ingredient ing = Ingredient.builder()
                .name(req.getName())
                .unit(req.getUnit())
                .stockQuantity(req.getStockQuantity())
                .active(true)
                .build();

        ingredientRepository.save(ing);

        // ✅ Audit log
        auditLogService.log(
                AuditAction.INGREDIENT_CREATE,
                "ingredient",
                ing.getId(),
                null,
                ing
        );
        return toResponse(ing);
    }

    /**
     * Cập nhật nguyên liệu.
     * Lấy theo ID → nếu không tồn tại thì báo lỗi.
     */
    public IngredientResponse update(Long id, IngredientUpdateRequest req) {

        Ingredient ing = ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        ing.setName(req.getName());
        ing.setUnit(req.getUnit());
        ing.setStockQuantity(req.getStockQuantity());
        ing.setActive(req.getActive());

        ingredientRepository.save(ing);

        // ✅ Audit log
        auditLogService.log(
                AuditAction.INGREDIENT_UPDATE,
                "ingredient",
                ing.getId(),
                null,
                ing
        );
        return toResponse(ing);
    }

    /**
     * Xóa nguyên liệu (xóa mềm).
     * Chỉ set active = false, không xóa khỏi DB.
     */
    public void delete(Long id) {
        Ingredient ing = ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        ing.setActive(false);
        ingredientRepository.save(ing);

        // ✅ Audit log
        auditLogService.log(
                AuditAction.INGREDIENT_DELETE,
                "ingredient",
                ing.getId(),
                ing,
                null
        );
    }

    /**
     * Convert Entity → DTO trả ra FE.
     */
    private IngredientResponse toResponse(Ingredient ing) {
        return IngredientResponse.builder()
                .id(ing.getId())
                .name(ing.getName())
                .unit(ing.getUnit())
                .stockQuantity(ing.getStockQuantity())
                .active(ing.getActive())
                .build();
    }
}
