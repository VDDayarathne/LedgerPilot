package com.ledgerpilot.ledgerpilot_backend.auth.domain;

import com.ledgerpilot.ledgerpilot_backend.identity.domain.UserAccount;
import com.ledgerpilot.ledgerpilot_backend.tenant.domain.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
@Getter
@Setter
@NoArgsConstructor
public class RefreshSession {
    @Id
    @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(nullable = false, length = 36, columnDefinition = "char(36)")
    private String familyId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_tenant_id")
    private Tenant activeTenant;
    @Column(nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String currentTokenHash;
    @Column(length = 64, columnDefinition = "char(64)")
    private String previousTokenHash;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant revokedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant lastUsedAt;
    @Version
    private long version;

    public RefreshSession(UserAccount user, Tenant tenant, String tokenHash, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.familyId = UUID.randomUUID().toString();
        this.user = user;
        this.activeTenant = tenant;
        this.currentTokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = this.lastUsedAt = Instant.now();
    }

    public boolean active() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
