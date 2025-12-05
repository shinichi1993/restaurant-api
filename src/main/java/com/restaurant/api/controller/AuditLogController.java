package com.restaurant.api.controller;

import com.restaurant.api.dto.audit.AuditLogResponse;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * API search audit log + phân trang
     * ----------------------------------------------------
     * GET /api/audit-logs?page=0&size=20&entity=user&action=USER_UPDATE&userId=3
     *
     * FE dùng cho Table filter.
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) List<AuditAction> actions,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AuditLogResponse> result =
                auditLogService.searchAuditLogs(entity, userId, actions, page, size);

        return ResponseEntity.ok(result);
    }
}
