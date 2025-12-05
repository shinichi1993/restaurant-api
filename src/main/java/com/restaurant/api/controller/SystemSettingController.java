package com.restaurant.api.controller;

import com.restaurant.api.dto.setting.SystemSettingResponse;
import com.restaurant.api.dto.setting.SystemSettingUpdateRequest;
import com.restaurant.api.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SystemSettingController
 * ----------------------------------------------------
 * - Cung cấp API cho màn hình Settings nâng cao (Module 20)
 * - Các API chính:
 *   + GET  /api/settings        → lấy toàn bộ cấu hình
 *   + GET  /api/settings/{group}→ lấy cấu hình theo group
 *   + PUT  /api/settings        → cập nhật nhiều cấu hình cùng lúc
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    /**
     * API lấy toàn bộ danh sách cấu hình.
     * ----------------------------------------------------
     * - FE có thể gọi 1 lần để load tất cả settings cho nhiều tab.
     */
    @GetMapping
    public ResponseEntity<List<SystemSettingResponse>> getAllSettings() {
        List<SystemSettingResponse> settings = systemSettingService.getAllSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * API lấy danh sách cấu hình theo group.
     * ----------------------------------------------------
     * - Ví dụ: /api/settings/POS, /api/settings/RESTAURANT
     * - Dùng khi FE muốn load từng tab riêng lẻ.
     */
    @GetMapping("/{group}")
    public ResponseEntity<List<SystemSettingResponse>> getSettingsByGroup(@PathVariable String group) {
        List<SystemSettingResponse> settings = systemSettingService.getSettingsByGroup(group);
        return ResponseEntity.ok(settings);
    }

    /**
     * API cập nhật nhiều cấu hình cùng lúc.
     * ----------------------------------------------------
     * - FE gửi danh sách cấu hình (key + value mới)
     * - BE sẽ validate kiểu dữ liệu và lưu lại
     */
    @PutMapping
    public ResponseEntity<Void> updateSettings(@RequestBody List<SystemSettingUpdateRequest> requests) {
        systemSettingService.updateSettings(requests);
        return ResponseEntity.ok().build();
    }
}
