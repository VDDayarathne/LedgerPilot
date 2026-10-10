package com.ledgerpilot.ledgerpilot_backend.tenant.domain;

import com.ledgerpilot.ledgerpilot_backend.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, unique = true, length = 100)
    private String slug;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;
    @Column(nullable = false, length = 2, columnDefinition = "char(2)")
    private String countryCode = "LK";
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String baseCurrency = "LKR";
    @Column(nullable = false)
    private short fiscalYearStartMonth = 1;

    public Tenant(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
