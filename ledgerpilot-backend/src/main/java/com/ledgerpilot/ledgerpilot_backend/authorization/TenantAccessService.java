package com.ledgerpilot.ledgerpilot_backend.authorization;

import com.ledgerpilot.ledgerpilot_backend.common.api.ApiException;
import com.ledgerpilot.ledgerpilot_backend.common.security.CurrentUser;
import com.ledgerpilot.ledgerpilot_backend.tenant.domain.*;
import com.ledgerpilot.ledgerpilot_backend.tenant.persistence.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {
    private final CurrentUser current;
    private final MembershipRepository memberships;

    public TenantAccessService(CurrentUser c, MembershipRepository m) {
        current = c;
        memberships = m;
    }

    public Membership require(Permission p) {
        String tid = current.tenantId();
        if (tid == null)
            throw new ApiException(HttpStatus.FORBIDDEN, "TENANT_REQUIRED", "Select an organization first.");
        Membership m = memberships.findByTenantIdAndUserId(tid, current.id()).filter(x -> x.getStatus() == MembershipStatus.ACTIVE && x.getTenant().getStatus() == TenantStatus.ACTIVE).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED", "Organization access denied."));
        if (!m.getRole().permissions().contains(p))
            throw new ApiException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "You do not have permission for this action.");
        return m;
    }
}
