package com.ledgerpilot.ledgerpilot_backend.audit.persistence;

import com.ledgerpilot.ledgerpilot_backend.audit.domain.AuditEvent;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    Page<AuditEvent> findAllByTenantId(String tenantId, Pageable pageable);
}
