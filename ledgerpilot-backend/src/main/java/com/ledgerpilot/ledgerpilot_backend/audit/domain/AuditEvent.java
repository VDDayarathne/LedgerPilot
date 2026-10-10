package com.ledgerpilot.ledgerpilot_backend.audit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@NoArgsConstructor
public class AuditEvent {
    @Id
    @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(name = "tenant_id", length = 36, columnDefinition = "char(36)")
    private String tenantId;
    @Column(name = "actor_user_id", length = 36, columnDefinition = "char(36)")
    private String actorUserId;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(nullable = false, length = 60)
    private String targetType;
    @Column(length = 80)
    private String targetId;
    @Column(length = 80)
    private String requestId;
    @Column(length = 64)
    private String ipAddress;
    @Column(length = 1000)
    private String details;
    @Column(nullable = false)
    private Instant occurredAt;

    public AuditEvent(String tenantId, String actor, String action, String targetType, String targetId, String requestId, String ip, String details) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.actorUserId = actor;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.requestId = requestId;
        this.ipAddress = ip;
        this.details = details;
        this.occurredAt = Instant.now();
    }
}
