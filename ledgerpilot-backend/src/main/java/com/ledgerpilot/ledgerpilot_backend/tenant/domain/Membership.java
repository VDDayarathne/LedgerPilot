package com.ledgerpilot.ledgerpilot_backend.tenant.domain;

import com.ledgerpilot.ledgerpilot_backend.authorization.Role;
import com.ledgerpilot.ledgerpilot_backend.common.domain.BaseEntity;
import com.ledgerpilot.ledgerpilot_backend.identity.domain.UserAccount;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Membership extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    public Membership(Tenant tenant, UserAccount user, Role role) {
        this.tenant = tenant;
        this.user = user;
        this.role = role;
    }
}
