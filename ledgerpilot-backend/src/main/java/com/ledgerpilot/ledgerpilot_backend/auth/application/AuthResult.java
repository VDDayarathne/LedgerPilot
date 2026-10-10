package com.ledgerpilot.ledgerpilot_backend.auth.application;

import com.ledgerpilot.ledgerpilot_backend.auth.domain.RefreshSession;

public record AuthResult(String accessToken, String refreshToken, String userId, String email, String displayName,
                         boolean emailVerified, String activeTenantId, String sessionId) {
    public static AuthResult of(String access, String refresh, RefreshSession s) {
        var u = s.getUser();
        return new AuthResult(access, refresh, u.getId(), u.getEmail(), u.getDisplayName(), u.isEmailVerified(), s.getActiveTenant() == null ? null : s.getActiveTenant().getId(), s.getId());
    }
}
