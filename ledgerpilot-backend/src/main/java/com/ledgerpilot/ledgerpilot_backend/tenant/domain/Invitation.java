package com.ledgerpilot.ledgerpilot_backend.tenant.domain;

import com.ledgerpilot.ledgerpilot_backend.authorization.Role;
import com.ledgerpilot.ledgerpilot_backend.identity.domain.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
public class Invitation {
    @Id
    @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @Column(nullable = false, length = 254)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    @Column(nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by")
    private UserAccount invitedBy;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant acceptedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Version
    private long version;

    public Invitation(Tenant tenant, String email, Role role, String tokenHash, UserAccount actor, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.tenant = tenant;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.invitedBy = actor;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
}
