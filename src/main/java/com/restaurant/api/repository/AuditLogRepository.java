package com.restaurant.api.repository;

import com.restaurant.api.entity.AuditLog;
import com.restaurant.api.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * AuditLogRepository
 * --------------------------------------------------------------
 * Repository thao tác với bảng audit_log.
 * Dùng để:
 *   - Lưu log
 *   - Query theo user / entity / thời gian
 *   - Sau này làm trang Audit Log FE (Module nâng cao)
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

}
