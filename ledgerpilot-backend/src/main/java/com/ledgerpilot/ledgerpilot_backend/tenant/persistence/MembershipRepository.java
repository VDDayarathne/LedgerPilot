package com.ledgerpilot.ledgerpilot_backend.tenant.persistence;

import com.ledgerpilot.ledgerpilot_backend.authorization.Role;
import com.ledgerpilot.ledgerpilot_backend.tenant.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface MembershipRepository extends JpaRepository<Membership, String> {
    Optional<Membership> findByTenantIdAndUserId(String tenantId, String userId);

    List<Membership> findAllByUserIdAndStatus(String userId, MembershipStatus status);

    Page<Membership> findAllByTenantId(String tenantId, Pageable pageable);

    long countByTenantIdAndRoleAndStatus(String tenantId, Role role, MembershipStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membership m where m.tenant.id=:tenantId and m.user.id=:userId")
    Optional<Membership> lockByTenantAndUser(String tenantId, String userId);
}
