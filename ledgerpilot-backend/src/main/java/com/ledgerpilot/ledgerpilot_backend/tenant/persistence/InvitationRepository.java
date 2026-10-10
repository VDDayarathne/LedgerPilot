package com.ledgerpilot.ledgerpilot_backend.tenant.persistence;

import com.ledgerpilot.ledgerpilot_backend.tenant.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface InvitationRepository extends JpaRepository<Invitation, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invitation> findByTokenHash(String hash);

    Optional<Invitation> findByIdAndTenantId(String id, String tenantId);

    Page<Invitation> findAllByTenantId(String tenantId, Pageable pageable);

    boolean existsByTenantIdAndEmailIgnoreCaseAndStatus(String tenantId, String email, InvitationStatus status);
}
