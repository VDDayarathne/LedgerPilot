package com.ledgerpilot.ledgerpilot_backend.auth.domain;

import com.ledgerpilot.ledgerpilot_backend.identity.domain.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "one_time_tokens")
@Getter
@Setter
@NoArgsConstructor
public class OneTimeToken {
    public enum Type {EMAIL_VERIFICATION, PASSWORD_RESET}

    @Id
    @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;
    @Column(nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String tokenHash;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant usedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Version
    private long version;

    public OneTimeToken(UserAccount user, Type type, String hash, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.type = type;
        this.tokenHash = hash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
}
