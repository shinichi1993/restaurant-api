package com.restaurant.api.controller;

import com.restaurant.api.service.BackupService;
import com.restaurant.api.service.RestoreService;
import com.restaurant.api.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backup")
public class AdminBackupController {

    private final BackupService backupService;
    private final RestoreService restoreService;

    /**
     * Export backup ZIP
     */
    @PreAuthorize("hasAuthority('ADMIN_BACKUP')")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export() {
        byte[] zipBytes = backupService.exportBackupZip();

        String fileName = "backup_" + LocalDateTime.now().toString().replace(":", "-") + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    /**
     * Restore backup ZIP
     */
    @PreAuthorize("hasAuthority('ADMIN_RESTORE')")
    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> restore(
            @RequestParam("file") MultipartFile file,
            @RequestParam("confirm") boolean confirm
    ) throws Exception {
        if (!confirm) {
            throw new IllegalArgumentException("Chưa xác nhận restore (confirm=false)");
        }

        restoreService.restoreFromZip(file.getInputStream());
        return ResponseEntity.ok().build();
    }
}
