package com.ledgerpilot.ledgerpilot_backend.tenant.persistence;

import com.ledgerpilot.ledgerpilot_backend.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, String> {
    boolean existsBySlug(String slug);
}
