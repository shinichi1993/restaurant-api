package com.restaurant.api.controller;

import com.restaurant.api.dto.audit.AuditLogResponse;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.restaurant.api.util.DateTimeUtil;
import java.time.LocalDateTime;

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
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) List<AuditAction> actions,
            @RequestParam(required = false) String fromDate, // dd/MM/yyyy HH:mm
            @RequestParam(required = false) String toDate, // dd/MM/yyyy HH:mm
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LocalDateTime from = DateTimeUtil.parse(fromDate);
        LocalDateTime to = DateTimeUtil.parse(toDate);
        Page<AuditLogResponse> result =
                auditLogService.searchAuditLogs(entity, username, actions, from, to, page, size);

        return ResponseEntity.ok(result);
    }
}
