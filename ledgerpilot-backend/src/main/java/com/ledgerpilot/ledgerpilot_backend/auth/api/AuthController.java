package com.ledgerpilot.ledgerpilot_backend.auth.api;

import com.ledgerpilot.ledgerpilot_backend.auth.application.*;
import com.ledgerpilot.ledgerpilot_backend.auth.config.AuthProperties;
import com.ledgerpilot.ledgerpilot_backend.common.security.*;
import com.ledgerpilot.ledgerpilot_backend.identity.domain.UserAccount;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthProperties props;
    private final CurrentUser current;
    private final FixedWindowRateLimiter limiter;

    public AuthController(AuthService a, AuthProperties p, CurrentUser c, FixedWindowRateLimiter l) {
        auth = a;
        props = p;
        current = c;
        limiter = l;
    }

    public record RegisterRequest(@Email @NotBlank String email, @Size(min = 12, max = 100) String password,
                                  @NotBlank @Size(max = 120) String displayName) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password, String tenantId) {
    }

    public record TokenRequest(@NotBlank String token) {
    }

    public record ResetRequest(@NotBlank String token, @Size(min = 12, max = 100) String password) {
    }

    public record EmailRequest(@Email @NotBlank String email) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @Size(min = 12, max = 100) String newPassword) {
    }

    public record ProfileRequest(@NotBlank @Size(max = 120) String displayName) {
    }

    public record Message(String message) {
    }

    public record UserView(String id, String email, String displayName, boolean emailVerified, String activeTenantId) {
    }

    public record SessionView(String accessToken, UserView user) {
    }

    @PostMapping("/register")
    public ResponseEntity<Message> register(@Valid @RequestBody RegisterRequest r, HttpServletRequest q) {
        limit(q, "register", 10);
        auth.register(r.email(), r.password(), r.displayName());
        return ResponseEntity.status(201).body(new Message("If the address can be used, a verification email has been sent."));
    }

    @PostMapping("/verify-email")
    public Message verify(@Valid @RequestBody TokenRequest r, HttpServletRequest q) {
        limit(q, "verify", 20);
        auth.verify(r.token());
        return new Message("Email verified. You can now sign in.");
    }

    @PostMapping("/login")
    public ResponseEntity<SessionView> login(@Valid @RequestBody LoginRequest r, HttpServletRequest q, HttpServletResponse s) {
        limit(q, "login", 10);
        return session(auth.login(r.email(), r.password(), r.tenantId()), s, true);
    }

    @PostMapping("/refresh")
    public ResponseEntity<SessionView> refresh(@CookieValue(name = "refresh_token", required = false) String raw, HttpServletRequest q, HttpServletResponse s) {
        limit(q, "refresh", 30);
        if (raw == null)
            throw new com.ledgerpilot.ledgerpilot_backend.common.api.ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Your session is invalid or expired.");
        return session(auth.refresh(raw), s, true);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String raw, HttpServletResponse s) {
        auth.logout(raw);
        clearCookies(s);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletResponse s) {
        auth.logoutAll(current.id());
        clearCookies(s);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public Message forgot(@Valid @RequestBody EmailRequest r, HttpServletRequest q) {
        limit(q, "forgot", 5);
        auth.forgot(r.email());
        return new Message("If the account exists, a reset email has been sent.");
    }

    @PostMapping("/reset-password")
    public Message reset(@Valid @RequestBody ResetRequest r, HttpServletRequest q) {
        limit(q, "reset", 10);
        auth.reset(r.token(), r.password());
        return new Message("Password reset. Sign in again on all devices.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> change(@Valid @RequestBody ChangePasswordRequest r, HttpServletResponse s) {
        auth.change(current.id(), r.currentPassword(), r.newPassword());
        clearCookies(s);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserView me() {
        UserAccount u = auth.user(current.id());
        return new UserView(u.getId(), u.getEmail(), u.getDisplayName(), u.isEmailVerified(), current.tenantId());
    }

    @PutMapping("/me")
    public UserView profile(@Valid @RequestBody ProfileRequest r) {
        UserAccount u = auth.updateProfile(current.id(), r.displayName());
        return new UserView(u.getId(), u.getEmail(), u.getDisplayName(), u.isEmailVerified(), current.tenantId());
    }

    private ResponseEntity<SessionView> session(AuthResult r, HttpServletResponse response, boolean refresh) {
        if (refresh && r.refreshToken() != null)
            response.addHeader(HttpHeaders.SET_COOKIE, cookie("refresh_token", r.refreshToken(), true, Duration.ofDays(props.refreshTokenDays())).toString());
        String xsrf = UUID.randomUUID().toString();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("XSRF-TOKEN", xsrf, false, Duration.ofDays(props.refreshTokenDays())).toString());
        return ResponseEntity.ok(new SessionView(r.accessToken(), new UserView(r.userId(), r.email(), r.displayName(), r.emailVerified(), r.activeTenantId())));
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration age) {
        String path = "XSRF-TOKEN".equals(name) ? "/" : "/api/v1/auth";
        return ResponseCookie.from(name, value).httpOnly(httpOnly).secure(props.cookieSecure()).sameSite(props.cookieSameSite()).path(path).maxAge(age).build();
    }

    private void clearCookies(HttpServletResponse r) {
        r.addHeader(HttpHeaders.SET_COOKIE, cookie("refresh_token", "", true, Duration.ZERO).toString());
        r.addHeader(HttpHeaders.SET_COOKIE, cookie("XSRF-TOKEN", "", false, Duration.ZERO).toString());
    }

    private void limit(HttpServletRequest q, String action, int max) {
        limiter.check(action + ":" + q.getRemoteAddr(), max, Duration.ofMinutes(1));
    }
}
