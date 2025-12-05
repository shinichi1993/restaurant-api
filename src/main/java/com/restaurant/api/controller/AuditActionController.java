package com.restaurant.api.controller;

import com.restaurant.api.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-actions")
@RequiredArgsConstructor
public class AuditActionController {
    /**
     * API search audit action
     * ----------------------------------------------------
     * GET /api/audit-actions
     *
     * FE dùng cho Action filter.
     */
    @GetMapping
    public ResponseEntity<AuditAction[]> getActions() {
        return ResponseEntity.ok(AuditAction.values());
    }
}
