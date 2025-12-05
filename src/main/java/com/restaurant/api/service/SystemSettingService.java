package com.restaurant.api.service;

import com.restaurant.api.dto.setting.SystemSettingResponse;
import com.restaurant.api.dto.setting.SystemSettingUpdateRequest;
import com.restaurant.api.entity.SystemSetting;
import com.restaurant.api.enums.SettingValueType;
import com.restaurant.api.repository.SystemSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ cho cấu hình hệ thống (system_setting)
 * --------------------------------------------------------------
 * - Đọc toàn bộ danh sách cấu hình để FE hiển thị
 * - Cập nhật giá trị cấu hình từ màn hình Settings nâng cao
 * - Kiểm tra kiểu dữ liệu cơ bản dựa trên valueType
 */
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    /**
     * Lấy toàn bộ cấu hình hệ thống.
     * Dùng cho màn hình Settings nâng cao, load lần đầu.
     */
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        List<SystemSetting> settings = systemSettingRepository.findAll();

        return settings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách cấu hình theo group (RESTAURANT, POS, LOYALTY...).
     * Có thể dùng nếu FE muốn load theo từng tab riêng.
     */
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getSettingsByGroup(String group) {
        List<SystemSetting> settings = systemSettingRepository.findBySettingGroup(group);

        return settings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật nhiều cấu hình cùng lúc.
     * - FE gửi lên một danh sách key + value mới
     * - Service sẽ:
     *   + Tìm record tương ứng
     *   + Kiểm tra kiểu dữ liệu
     *   + Ghi nhận updatedAt
     */
    @Transactional
    public void updateSettings(List<SystemSettingUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        for (SystemSettingUpdateRequest req : requests) {
            // Tìm setting theo key (ưu tiên key vì unique)
            SystemSetting setting = systemSettingRepository.findBySettingKey(req.getSettingKey())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy cấu hình với key: " + req.getSettingKey()
                    ));

            // Kiểm tra kiểu dữ liệu dựa trên valueType trước khi ghi
            validateSettingValue(setting.getValueType(), req.getSettingValue(), setting.getSettingKey());

            // Cập nhật giá trị
            setting.setSettingValue(req.getSettingValue());
            setting.setUpdatedAt(LocalDateTime.now());
            // TODO: Sau này có thể set updatedBy từ user đang đăng nhập

            systemSettingRepository.save(setting);
        }
    }

    /**
     * Hàm convert Entity → DTO trả về cho FE.
     */
    private SystemSettingResponse mapToResponse(SystemSetting setting) {
        return SystemSettingResponse.builder()
                .id(setting.getId())
                .settingGroup(setting.getSettingGroup())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .valueType(setting.getValueType())
                .description(setting.getDescription())
                .build();
    }

    /**
     * Hàm kiểm tra kiểu dữ liệu cơ bản trước khi lưu.
     * ----------------------------------------------------
     * - NUMBER  → parse BigDecimal, nếu lỗi thì throw
     * - BOOLEAN → chỉ chấp nhận true/false (không phân biệt hoa thường)
     * - STRING  → luôn hợp lệ
     * - JSON    → tạm thời không validate, có thể bổ sung sau
     */
    private void validateSettingValue(SettingValueType type, String rawValue, String settingKey) {
        if (rawValue == null) {
            return;
        }

        switch (type) {
            case NUMBER -> {
                try {
                    new BigDecimal(rawValue);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Giá trị của cấu hình " + settingKey + " phải là số hợp lệ."
                    );
                }
            }
            case BOOLEAN -> {
                String lower = rawValue.toLowerCase();
                if (!"true".equals(lower) && !"false".equals(lower)) {
                    throw new IllegalArgumentException(
                            "Giá trị của cấu hình " + settingKey + " phải là true hoặc false."
                    );
                }
            }
            case STRING -> {
                // STRING luôn hợp lệ, không cần validate gì thêm
            }
            case JSON -> {
                // Tạm thời chưa validate JSON để đơn giản,
                // có thể bổ sung ObjectMapper để parse sau.
            }
        }
    }
    /**
     * Lấy cấu hình dạng số (NUMBER) theo key.
     * ----------------------------------------------------
     * - Nếu không tìm thấy cấu hình → trả về defaultValue
     * - Nếu giá trị không phải số hợp lệ → trả về defaultValue
     *
     * @param key          khóa cấu hình (vd: discount.default_percent)
     * @param defaultValue giá trị mặc định nếu không tìm thấy hoặc lỗi parse
     */
    @Transactional(readOnly = true)
    public java.math.BigDecimal getNumberSetting(String key, java.math.BigDecimal defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> {
                    try {
                        return new java.math.BigDecimal(s.getSettingValue());
                    } catch (NumberFormatException ex) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Lấy cấu hình dạng boolean (BOOLEAN) theo key.
     * ----------------------------------------------------
     * - Nếu không tìm thấy → trả về defaultValue
     * - Giá trị chấp nhận: "true" hoặc "false" (không phân biệt hoa thường)
     *
     * @param key          khóa cấu hình
     * @param defaultValue giá trị mặc định nếu không tìm thấy
     */
    @Transactional(readOnly = true)
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> {
                    String raw = s.getSettingValue();
                    if (raw == null) {
                        return defaultValue;
                    }
                    String lower = raw.trim().toLowerCase();
                    if ("true".equals(lower)) return true;
                    if ("false".equals(lower)) return false;
                    return defaultValue;
                })
                .orElse(defaultValue);
    }


}
