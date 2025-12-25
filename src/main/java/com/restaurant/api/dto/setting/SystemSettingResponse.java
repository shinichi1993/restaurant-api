package com.restaurant.api.dto.setting;

import com.restaurant.api.enums.SettingValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về cho FE khi lấy danh sách cấu hình
 * ----------------------------------------------------
 * - Không trả trực tiếp entity để tránh lộ các field không cần thiết
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingResponse {

    private Long id;

    // Nhóm cấu hình (RESTAURANT, POS, LOYALTY, REPORT...)
    private String settingGroup;

    // Khóa cấu hình (restaurant.name, vat.rate...)
    private String settingKey;

    // Giá trị cấu hình dạng text (FE sẽ bind vào form)
    private String settingValue;

    // Kiểu dữ liệu logic
    private SettingValueType valueType;

    // Mô tả hiển thị trên màn hình cấu hình
    private String description;

    // =========================
    // Metadata cho FE render động
    // =========================
    private String label;          // tên hiển thị
    private String inputType;      // INPUT/NUMBER/SWITCH/...
    private Integer orderIndex;    // thứ tự
    private java.math.BigDecimal minValue;
    private java.math.BigDecimal maxValue;
    private Boolean visible;       // ẩn/hiện
    private Boolean editable;      // disable input
    private String dependsOnKey;   // dependency key
    private String dependsOnValue; // dependency value
    private String settingGroupLabel;   // label hiển thị FE của settingGroup: Cấu hình POS,...
}
