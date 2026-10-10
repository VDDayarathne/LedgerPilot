package com.ledgerpilot.ledgerpilot_backend.identity.domain;

import com.ledgerpilot.ledgerpilot_backend.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount extends BaseEntity {
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 120)
    private String displayName;
    @Column(nullable = false)
    private boolean emailVerified;
    @Column(nullable = false)
    private boolean enabled = true;

    public UserAccount(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }
}
