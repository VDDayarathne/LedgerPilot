package com.ledgerpilot.ledgerpilot_backend.common.security;

import com.ledgerpilot.ledgerpilot_backend.common.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public String id() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (!(a instanceof JwtAuthenticationToken j))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
        return j.getToken().getSubject();
    }

    public String tenantId() {
        var a = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return a.getToken().getClaimAsString("tid");
    }

    public String sessionId() {
        var a = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return a.getToken().getClaimAsString("sid");
    }
}
