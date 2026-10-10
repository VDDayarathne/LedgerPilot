package com.ledgerpilot.ledgerpilot_backend.tenant.api;

import com.ledgerpilot.ledgerpilot_backend.auth.api.AuthController.*;
import com.ledgerpilot.ledgerpilot_backend.auth.application.*;
import com.ledgerpilot.ledgerpilot_backend.authorization.*;
import com.ledgerpilot.ledgerpilot_backend.common.security.CurrentUser;
import com.ledgerpilot.ledgerpilot_backend.tenant.application.TenantService;
import com.ledgerpilot.ledgerpilot_backend.tenant.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class TenantController {
    private final TenantService service;
    private final AuthService auth;
    private final CurrentUser current;

    public TenantController(TenantService s, AuthService a, CurrentUser c) {
        service = s;
        auth = a;
        current = c;
    }

    public record CreateTenant(@NotBlank @Size(max = 160) String name) {
    }

    public record UpdateTenant(@NotBlank @Size(max = 160) String name,
                               @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
                               @Min(1) @Max(12) short fiscalYearStartMonth) {
    }

    public record TenantView(String id, String name, String slug, String status, String countryCode,
                             String baseCurrency, short fiscalYearStartMonth, String role) {
    }

    public record MemberView(String id, String userId, String email, String displayName, Role role,
                             MembershipStatus status, Instant createdAt) {
    }

    public record InviteRequest(@Email @NotBlank String email, @NotNull Role role) {
    }

    public record RoleRequest(@NotNull Role role) {
    }

    public record StatusRequest(@NotNull MembershipStatus status) {
    }

    public record InviteView(String id, String email, Role role, InvitationStatus status, Instant expiresAt,
                             Instant createdAt) {
    }

    public record InvitationPreview(String tenantName, String maskedEmail, Role role, Instant expiresAt) {
    }

    @PostMapping("/organizations")
    public ResponseEntity<TenantView> create(@Valid @RequestBody CreateTenant r) {
        return ResponseEntity.status(201).body(view(service.create(r.name()), Role.OWNER));
    }

    @GetMapping("/organizations")
    public List<TenantView> mine() {
        return service.mine().stream().map(m -> view(m.getTenant(), m.getRole())).toList();
    }

    @PostMapping("/organizations/{id}/select")
    public SessionView select(@PathVariable String id) {
        AuthResult r = auth.switchTenant(current.id(), current.sessionId(), id);
        return new SessionView(r.accessToken(), new UserView(r.userId(), r.email(), r.displayName(), r.emailVerified(), r.activeTenantId()));
    }

    @GetMapping("/organization")
    public TenantView get() {
        Tenant t = service.currentTenant();
        Role role = service.mine().stream().filter(m -> m.getTenant().getId().equals(t.getId())).findFirst().orElseThrow().getRole();
        return view(t, role);
    }

    @PutMapping("/organization")
    public TenantView update(@Valid @RequestBody UpdateTenant r) {
        Tenant t = service.update(r.name(), r.countryCode(), r.fiscalYearStartMonth());
        return view(t, null);
    }

    @GetMapping("/members")
    public Page<MemberView> members(@PageableDefault(size = 20, sort = "createdAt") Pageable p) {
        return service.members(p).map(this::member);
    }

    @PatchMapping("/members/{id}/role")
    public ResponseEntity<Void> role(@PathVariable String id, @Valid @RequestBody RoleRequest r) {
        service.changeRole(id, r.role());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/members/{id}/status")
    public ResponseEntity<Void> status(@PathVariable String id, @Valid @RequestBody StatusRequest r) {
        service.status(id, r.status());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/members/{id}/transfer-ownership")
    public ResponseEntity<Void> transfer(@PathVariable String id) {
        service.transfer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations")
    public Page<InviteView> invitations(@PageableDefault(size = 20, sort = "createdAt") Pageable p) {
        return service.invites(p).map(this::invite);
    }

    @PostMapping("/invitations")
    public ResponseEntity<Void> invite(@Valid @RequestBody InviteRequest r) {
        service.invite(r.email(), r.role());
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/invitations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations/preview")
    public InvitationPreview preview(@RequestParam String token) {
        Invitation i = service.preview(token);
        String e = i.getEmail();
        int at = e.indexOf('@');
        String masked = at > 1 ? e.charAt(0) + "***" + e.substring(at) : "***";
        return new InvitationPreview(i.getTenant().getName(), masked, i.getRole(), i.getExpiresAt());
    }

    @PostMapping("/invitations/accept")
    public TenantView accept(@RequestBody @Valid TokenRequest r) {
        return view(service.accept(r.token()), null);
    }

    @GetMapping("/permissions")
    public Map<Role, Set<Permission>> permissions() {
        Map<Role, Set<Permission>> map = new EnumMap<>(Role.class);
        for (Role r : Role.values()) map.put(r, r.permissions());
        return map;
    }

    private TenantView view(Tenant t, Role role) {
        return new TenantView(t.getId(), t.getName(), t.getSlug(), t.getStatus().name(), t.getCountryCode(), t.getBaseCurrency(), t.getFiscalYearStartMonth(), role == null ? null : role.name());
    }

    private MemberView member(Membership m) {
        return new MemberView(m.getId(), m.getUser().getId(), m.getUser().getEmail(), m.getUser().getDisplayName(), m.getRole(), m.getStatus(), m.getCreatedAt());
    }

    private InviteView invite(Invitation i) {
        return new InviteView(i.getId(), i.getEmail(), i.getRole(), i.getStatus(), i.getExpiresAt(), i.getCreatedAt());
    }
}
