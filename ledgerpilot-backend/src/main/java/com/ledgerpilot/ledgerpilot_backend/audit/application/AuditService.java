package com.ledgerpilot.ledgerpilot_backend.audit.application;

import com.ledgerpilot.ledgerpilot_backend.audit.domain.AuditEvent;
import com.ledgerpilot.ledgerpilot_backend.audit.persistence.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditEventRepository repo;
    private final HttpServletRequest request;

    public AuditService(AuditEventRepository r, HttpServletRequest q) {
        repo = r;
        request = q;
    }

    public void record(String tenant, String actor, String action, String type, String target, String details) {
        String rid = request.getHeader("X-Request-ID");
        String ip = request.getRemoteAddr();
        repo.save(new AuditEvent(tenant, actor, action, type, target, rid, ip, details));
    }
}
