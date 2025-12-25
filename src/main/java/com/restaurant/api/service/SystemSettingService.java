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
     * Lấy toàn bộ system setting để FE render động.
     *
     * Quy tắc xử lý:
     * ----------------------------------------------------
     * 1. Chỉ hiển thị setting có visible = true hoặc null
     *    (null = dữ liệu cũ, coi như hiển thị)
     *
     * 2. Sắp xếp thứ tự hiển thị:
     *    - Ưu tiên theo settingGroup (chia tab ổn định)
     *    - Sau đó theo orderIndex (thứ tự trong tab)
     *    - Cuối cùng fallback theo settingKey
     *      để đảm bảo kết quả luôn ổn định (deterministic)
     */
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                // 1️⃣ Ẩn các setting bị đánh dấu visible = false
                .filter(s -> Boolean.TRUE.equals(s.getVisible()))
                // 2️⃣ Sắp xếp để FE hiển thị ổn định
                .sorted((a, b) -> {
                    // 2.1. So sánh theo group (POS, LOYALTY, ...)
                    int ga = a.getSettingGroup() == null ? 0 : a.getSettingGroup().compareToIgnoreCase(b.getSettingGroup());
                    if (ga != 0) return ga;
                    // 2.2. So sánh theo orderIndex trong cùng group
                    Integer oa = a.getOrderIndex() == null ? 0 : a.getOrderIndex();
                    Integer ob = b.getOrderIndex() == null ? 0 : b.getOrderIndex();
                    int od = oa.compareTo(ob);
                    if (od != 0) return od;
                    // 2.3. Fallback theo key để tránh thứ tự không ổn định
                    String ka = a.getSettingKey() == null ? "" : a.getSettingKey();
                    String kb = b.getSettingKey() == null ? "" : b.getSettingKey();
                    return ka.compareToIgnoreCase(kb);
                })
                // 3️⃣ Convert Entity → DTO cho FE
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
                .settingGroupLabel(
                        setting.getSettingGroupLabel() != null
                                ? setting.getSettingGroupLabel()
                                : setting.getSettingGroup()   // fallback
                )
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .valueType(setting.getValueType())
                .description(setting.getDescription())
                .label(setting.getLabel())
                .inputType(setting.getInputType())
                .orderIndex(setting.getOrderIndex())
                .minValue(setting.getMinValue())
                .maxValue(setting.getMaxValue())
                .visible(setting.getVisible())
                .editable(setting.getEditable())
                .dependsOnKey(setting.getDependsOnKey())
                .dependsOnValue(setting.getDependsOnValue())
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

    /**
     * Lấy cấu hình dạng chuỗi (STRING) theo key.
     * ----------------------------------------------------
     * - Dùng cho các setting kiểu text tự do, ví dụ:
     *   + invoice.print_layout   → "A5" hoặc "THERMAL_80"
     *   + restaurant.name        → tên nhà hàng
     * - Nếu không tìm thấy cấu hình → trả về defaultValue
     *
     * @param key          khóa cấu hình (setting_key trong bảng system_setting)
     * @param defaultValue giá trị mặc định nếu không tìm thấy
     * @return giá trị setting dạng String
     */
    @Transactional(readOnly = true)
    public String getStringSetting(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

}
