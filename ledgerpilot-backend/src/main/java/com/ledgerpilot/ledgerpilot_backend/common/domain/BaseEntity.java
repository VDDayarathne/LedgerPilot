package com.ledgerpilot.ledgerpilot_backend.common.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
public abstract class BaseEntity {
    @Id
    @Column(length = 36, columnDefinition = "char(36)", updatable = false, nullable = false)
    protected String id;
    @Column(nullable = false, updatable = false)
    protected Instant createdAt;
    @Column(nullable = false)
    protected Instant updatedAt;
    @Version
    protected long version;

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void update() {
        updatedAt = Instant.now();
    }
}
