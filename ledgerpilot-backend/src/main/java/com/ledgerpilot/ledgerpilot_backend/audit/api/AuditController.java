package com.ledgerpilot.ledgerpilot_backend.audit.api;

import com.ledgerpilot.ledgerpilot_backend.audit.domain.AuditEvent;
import com.ledgerpilot.ledgerpilot_backend.audit.persistence.AuditEventRepository;
import com.ledgerpilot.ledgerpilot_backend.authorization.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditController {
    private final AuditEventRepository events;
    private final TenantAccessService access;

    public AuditController(AuditEventRepository e, TenantAccessService a) {
        events = e;
        access = a;
    }

    @GetMapping
    public Page<AuditEvent> list(@PageableDefault(size = 25, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable p) {
        String tid = access.require(Permission.AUDIT_READ).getTenant().getId();
        return events.findAllByTenantId(tid, p);
    }
}
