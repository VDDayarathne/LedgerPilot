package com.ledgerpilot.ledgerpilot_backend.auth.application;

import com.ledgerpilot.ledgerpilot_backend.audit.application.AuditService;
import com.ledgerpilot.ledgerpilot_backend.auth.config.AuthProperties;
import com.ledgerpilot.ledgerpilot_backend.auth.domain.*;
import com.ledgerpilot.ledgerpilot_backend.auth.persistence.*;
import com.ledgerpilot.ledgerpilot_backend.common.api.ApiException;
import com.ledgerpilot.ledgerpilot_backend.identity.domain.UserAccount;
import com.ledgerpilot.ledgerpilot_backend.identity.persistence.UserRepository;
import com.ledgerpilot.ledgerpilot_backend.tenant.domain.*;
import com.ledgerpilot.ledgerpilot_backend.tenant.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final TenantRepository tenants;
    private final RefreshSessionRepository sessions;
    private final OneTimeTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final TokenCodec codec;
    private final JwtService jwt;
    private final MailDelivery mail;
    private final AuthProperties props;
    private final AuditService audit;

    public AuthService(UserRepository u, MembershipRepository m, TenantRepository t, RefreshSessionRepository s, OneTimeTokenRepository o, PasswordEncoder p, TokenCodec c, JwtService j, MailDelivery d, AuthProperties a, AuditService au) {
        users = u;
        memberships = m;
        tenants = t;
        sessions = s;
        tokens = o;
        passwords = p;
        codec = c;
        jwt = j;
        mail = d;
        props = a;
        audit = au;
    }

    @Transactional
    public void register(String email, String password, String name) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalized)) return;
        UserAccount user = users.save(new UserAccount(normalized, passwords.encode(password), name.trim()));
        issueOneTime(user, OneTimeToken.Type.EMAIL_VERIFICATION, Duration.ofHours(24));
        audit.record(null, user.getId(), "AUTH_REGISTERED", "USER", user.getId(), null);
    }

    @Transactional
    public void verify(String raw) {
        OneTimeToken t = validToken(raw, OneTimeToken.Type.EMAIL_VERIFICATION);
        t.setUsedAt(Instant.now());
        t.getUser().setEmailVerified(true);
        audit.record(null, t.getUser().getId(), "EMAIL_VERIFIED", "USER", t.getUser().getId(), null);
    }

    @Transactional
    public AuthResult login(String email, String password, String requestedTenant) {
        UserAccount user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(this::badCredentials);
        if (!user.isEnabled() || !passwords.matches(password, user.getPasswordHash())) throw badCredentials();
        if (!user.isEmailVerified())
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Verify your email before signing in.");
        Tenant selected = selectTenant(user.getId(), requestedTenant);
        String raw = codec.random();
        RefreshSession session = sessions.save(new RefreshSession(user, selected, codec.hash(raw), Instant.now().plus(Duration.ofDays(props.refreshTokenDays()))));
        audit.record(selected == null ? null : selected.getId(), user.getId(), "AUTH_LOGIN", "SESSION", session.getId(), null);
        return AuthResult.of(jwt.accessToken(user, selected == null ? null : selected.getId(), session.getId()), raw, session);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String raw) {
        String hash = codec.hash(raw);
        RefreshSession s = sessions.lockByTokenHash(hash).orElseThrow(this::invalidSession);
        if (hash.equals(s.getPreviousTokenHash())) {
            sessions.revokeFamily(s.getFamilyId(), Instant.now());
            audit.record(s.getActiveTenant() == null ? null : s.getActiveTenant().getId(), s.getUser().getId(), "REFRESH_REUSE_DETECTED", "SESSION", s.getId(), null);
            throw invalidSession();
        }
        validateSession(s);
        validateActiveTenant(s);
        String next = codec.random();
        s.setPreviousTokenHash(s.getCurrentTokenHash());
        s.setCurrentTokenHash(codec.hash(next));
        s.setLastUsedAt(Instant.now());
        return AuthResult.of(jwt.accessToken(s.getUser(), s.getActiveTenant() == null ? null : s.getActiveTenant().getId(), s.getId()), next, s);
    }

    @Transactional
    public void logout(String raw) {
        if (raw == null) return;
        sessions.lockByTokenHash(codec.hash(raw)).ifPresent(s -> {
            s.setRevokedAt(Instant.now());
            audit.record(s.getActiveTenant() == null ? null : s.getActiveTenant().getId(), s.getUser().getId(), "AUTH_LOGOUT", "SESSION", s.getId(), null);
        });
    }

    @Transactional
    public void logoutAll(String userId) {
        sessions.revokeAllForUser(userId, Instant.now());
        audit.record(null, userId, "AUTH_LOGOUT_ALL", "USER", userId, null);
    }

    @Transactional
    public void forgot(String email) {
        users.findByEmailIgnoreCase(email.trim()).ifPresent(u -> issueOneTime(u, OneTimeToken.Type.PASSWORD_RESET, Duration.ofMinutes(30)));
    }

    @Transactional
    public void reset(String raw, String password) {
        OneTimeToken t = validToken(raw, OneTimeToken.Type.PASSWORD_RESET);
        t.setUsedAt(Instant.now());
        t.getUser().setPasswordHash(passwords.encode(password));
        sessions.revokeAllForUser(t.getUser().getId(), Instant.now());
        audit.record(null, t.getUser().getId(), "PASSWORD_RESET", "USER", t.getUser().getId(), null);
    }

    @Transactional
    public void change(String userId, String current, String replacement) {
        UserAccount u = users.findById(userId).orElseThrow(this::invalidSession);
        if (!passwords.matches(current, u.getPasswordHash()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "Current password is incorrect.");
        u.setPasswordHash(passwords.encode(replacement));
        sessions.revokeAllForUser(userId, Instant.now());
        audit.record(null, userId, "PASSWORD_CHANGED", "USER", userId, null);
    }

    @Transactional(readOnly = true)
    public UserAccount user(String id) {
        return users.findById(id).orElseThrow(this::invalidSession);
    }

    @Transactional
    public UserAccount updateProfile(String userId, String displayName) {
        UserAccount user = users.findById(userId).orElseThrow(this::invalidSession);
        user.setDisplayName(displayName.trim());
        audit.record(null, userId, "PROFILE_UPDATED", "USER", userId, null);
        return user;
    }

    @Transactional
    public AuthResult switchTenant(String userId, String sessionId, String tenantId) {
        RefreshSession s = sessions.findByIdAndUserId(sessionId, userId).orElseThrow(this::invalidSession);
        validateSession(s);
        Membership m = memberships.findByTenantIdAndUserId(tenantId, userId).filter(x -> x.getStatus() == MembershipStatus.ACTIVE && x.getTenant().getStatus() == TenantStatus.ACTIVE).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED", "You do not have active access to this organization."));
        s.setActiveTenant(m.getTenant());
        s.setLastUsedAt(Instant.now());
        audit.record(tenantId, userId, "TENANT_SWITCHED", "TENANT", tenantId, null);
        return AuthResult.of(jwt.accessToken(s.getUser(), tenantId, s.getId()), null, s);
    }

    private Tenant selectTenant(String userId, String requested) {
        List<Membership> active = memberships.findAllByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream().filter(m -> m.getTenant().getStatus() == TenantStatus.ACTIVE).toList();
        if (requested != null && !requested.isBlank())
            return active.stream().filter(m -> m.getTenant().getId().equals(requested)).map(Membership::getTenant).findFirst().orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED", "Organization access denied."));
        return active.size() == 1 ? active.getFirst().getTenant() : null;
    }

    private void issueOneTime(UserAccount u, OneTimeToken.Type type, Duration duration) {
        tokens.findAllByUserIdAndTypeAndUsedAtIsNull(u.getId(), type).forEach(t -> t.setUsedAt(Instant.now()));
        String raw = codec.random();
        tokens.save(new OneTimeToken(u, type, codec.hash(raw), Instant.now().plus(duration)));
        if (type == OneTimeToken.Type.EMAIL_VERIFICATION) mail.verification(u.getEmail(), raw);
        else mail.passwordReset(u.getEmail(), raw);
    }

    private OneTimeToken validToken(String raw, OneTimeToken.Type type) {
        OneTimeToken t = tokens.findByTokenHashAndType(codec.hash(raw), type).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "The link is invalid or expired."));
        if (t.getUsedAt() != null || !t.getExpiresAt().isAfter(Instant.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "The link is invalid or expired.");
        return t;
    }

    private void validateSession(RefreshSession s) {
        if (!s.active() || !s.getUser().isEnabled()) throw invalidSession();
    }

    private void validateActiveTenant(RefreshSession s) {
        if (s.getActiveTenant() != null) {
            memberships.findByTenantIdAndUserId(s.getActiveTenant().getId(), s.getUser().getId()).filter(m -> m.getStatus() == MembershipStatus.ACTIVE && m.getTenant().getStatus() == TenantStatus.ACTIVE).orElseThrow(this::invalidSession);
        }
    }

    private ApiException badCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
    }

    private ApiException invalidSession() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Your session is invalid or expired.");
    }
}
