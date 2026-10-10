package com.ledgerpilot.ledgerpilot_backend.tenant.application;

import com.ledgerpilot.ledgerpilot_backend.audit.application.AuditService;
import com.ledgerpilot.ledgerpilot_backend.auth.application.*;
import com.ledgerpilot.ledgerpilot_backend.authorization.*;
import com.ledgerpilot.ledgerpilot_backend.common.api.ApiException;
import com.ledgerpilot.ledgerpilot_backend.common.security.CurrentUser;
import com.ledgerpilot.ledgerpilot_backend.identity.persistence.UserRepository;
import com.ledgerpilot.ledgerpilot_backend.tenant.domain.*;
import com.ledgerpilot.ledgerpilot_backend.tenant.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class TenantService {
    private final TenantRepository tenants;
    private final MembershipRepository memberships;
    private final InvitationRepository invitations;
    private final UserRepository users;
    private final CurrentUser current;
    private final TenantAccessService access;
    private final TokenCodec tokens;
    private final MailDelivery mail;
    private final AuditService audit;

    public TenantService(TenantRepository t, MembershipRepository m, InvitationRepository i, UserRepository u, CurrentUser c, TenantAccessService a, TokenCodec x, MailDelivery d, AuditService au) {
        tenants = t;
        memberships = m;
        invitations = i;
        users = u;
        current = c;
        access = a;
        tokens = x;
        mail = d;
        audit = au;
    }

    @Transactional
    public Tenant create(String name) {
        var user = users.findById(current.id()).orElseThrow();
        if (!user.isEmailVerified())
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Verify your email first.");
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "organization";
        String slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        Tenant t = tenants.save(new Tenant(name.trim(), slug));
        memberships.save(new Membership(t, user, Role.OWNER));
        audit.record(t.getId(), user.getId(), "TENANT_CREATED", "TENANT", t.getId(), null);
        return t;
    }

    @Transactional(readOnly = true)
    public List<Membership> mine() {
        return memberships.findAllByUserIdAndStatus(current.id(), MembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Tenant currentTenant() {
        return access.require(Permission.TENANT_READ).getTenant();
    }

    @Transactional
    public Tenant update(String name, String country, short fiscalMonth) {
        Membership actor = access.require(Permission.TENANT_UPDATE);
        if (fiscalMonth < 1 || fiscalMonth > 12)
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FISCAL_MONTH", "Fiscal year start must be between 1 and 12.");
        Tenant t = actor.getTenant();
        t.setName(name.trim());
        t.setCountryCode(country.toUpperCase(Locale.ROOT));
        t.setFiscalYearStartMonth(fiscalMonth);
        audit.record(t.getId(), current.id(), "TENANT_UPDATED", "TENANT", t.getId(), null);
        return t;
    }

    @Transactional(readOnly = true)
    public Page<Membership> members(Pageable p) {
        String tid = access.require(Permission.MEMBER_READ).getTenant().getId();
        return memberships.findAllByTenantId(tid, p);
    }

    @Transactional(readOnly = true)
    public Page<Invitation> invites(Pageable p) {
        String tid = access.require(Permission.MEMBER_READ).getTenant().getId();
        return invitations.findAllByTenantId(tid, p);
    }

    @Transactional
    public void invite(String email, Role role) {
        Membership actor = access.require(Permission.MEMBER_INVITE);
        assertAssignable(actor, role);
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String tid = actor.getTenant().getId();
        if (memberships.findByTenantIdAndUserId(tid, users.findByEmailIgnoreCase(normalized).map(u -> u.getId()).orElse("-")).filter(m -> m.getStatus() == MembershipStatus.ACTIVE).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_MEMBER", "This person is already a member.");
        if (invitations.existsByTenantIdAndEmailIgnoreCaseAndStatus(tid, normalized, InvitationStatus.PENDING))
            throw new ApiException(HttpStatus.CONFLICT, "INVITATION_EXISTS", "A pending invitation already exists.");
        String raw = tokens.random();
        Invitation i = invitations.save(new Invitation(actor.getTenant(), normalized, role, tokens.hash(raw), actor.getUser(), Instant.now().plus(Duration.ofDays(7))));
        mail.invitation(normalized, raw, actor.getTenant().getName());
        audit.record(tid, current.id(), "INVITATION_CREATED", "INVITATION", i.getId(), "role=" + role);
    }

    @Transactional
    public void cancel(String id) {
        Membership actor = access.require(Permission.MEMBER_INVITE);
        Invitation i = invitations.findByIdAndTenantId(id, actor.getTenant().getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Invitation not found."));
        if (i.getStatus() != InvitationStatus.PENDING)
            throw new ApiException(HttpStatus.CONFLICT, "INVITATION_NOT_PENDING", "Invitation is no longer pending.");
        i.setStatus(InvitationStatus.CANCELLED);
        audit.record(actor.getTenant().getId(), current.id(), "INVITATION_CANCELLED", "INVITATION", id, null);
    }

    @Transactional(readOnly = true)
    public Invitation preview(String raw) {
        Invitation i = invitations.findByTokenHash(tokens.hash(raw)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_INVITATION", "Invitation is invalid or expired."));
        if (i.getStatus() != InvitationStatus.PENDING || !i.getExpiresAt().isAfter(Instant.now()))
            throw new ApiException(HttpStatus.GONE, "INVALID_INVITATION", "Invitation is invalid or expired.");
        return i;
    }

    @Transactional
    public Tenant accept(String raw) {
        Invitation i = invitations.findByTokenHash(tokens.hash(raw)).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVITATION", "Invitation is invalid or expired."));
        if (i.getStatus() != InvitationStatus.PENDING || !i.getExpiresAt().isAfter(Instant.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVITATION", "Invitation is invalid or expired.");
        var user = users.findById(current.id()).orElseThrow();
        if (!user.isEmailVerified() || !user.getEmail().equalsIgnoreCase(i.getEmail()))
            throw new ApiException(HttpStatus.FORBIDDEN, "INVITATION_EMAIL_MISMATCH", "Sign in with the verified email address that was invited.");
        Membership m = memberships.findByTenantIdAndUserId(i.getTenant().getId(), user.getId()).orElse(null);
        if (m == null) memberships.save(new Membership(i.getTenant(), user, i.getRole()));
        else {
            m.setRole(i.getRole());
            m.setStatus(MembershipStatus.ACTIVE);
        }
        i.setStatus(InvitationStatus.ACCEPTED);
        i.setAcceptedAt(Instant.now());
        audit.record(i.getTenant().getId(), user.getId(), "INVITATION_ACCEPTED", "INVITATION", i.getId(), null);
        return i.getTenant();
    }

    @Transactional
    public void changeRole(String memberId, Role role) {
        Membership actor = access.require(Permission.MEMBER_MANAGE);
        Membership target = memberInTenant(memberId, actor.getTenant().getId());
        assertCanManage(actor, target, role);
        if (target.getRole() == Role.OWNER) protectFinalOwner(target);
        target.setRole(role);
        audit.record(actor.getTenant().getId(), current.id(), "MEMBERSHIP_ROLE_CHANGED", "MEMBERSHIP", memberId, "role=" + role);
    }

    @Transactional
    public void status(String memberId, MembershipStatus status) {
        Membership actor = access.require(Permission.MEMBER_MANAGE);
        Membership target = memberInTenant(memberId, actor.getTenant().getId());
        assertCanManage(actor, target, target.getRole());
        if (status == MembershipStatus.ACTIVE && target.getStatus() == MembershipStatus.REMOVED)
            throw new ApiException(HttpStatus.CONFLICT, "MEMBERSHIP_REMOVED", "Removed memberships cannot be reactivated; send a new invitation.");
        if (target.getRole() == Role.OWNER && status != MembershipStatus.ACTIVE) protectFinalOwner(target);
        target.setStatus(status);
        audit.record(actor.getTenant().getId(), current.id(), "MEMBERSHIP_STATUS_CHANGED", "MEMBERSHIP", memberId, "status=" + status);
    }

    @Transactional
    public void transfer(String targetId) {
        Membership actor = access.require(Permission.OWNER_TRANSFER);
        String tid = actor.getTenant().getId();
        Membership lockedActor = memberships.lockByTenantAndUser(tid, current.id()).orElseThrow();
        if (lockedActor.getRole() != Role.OWNER)
            throw new ApiException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Only the current owner can transfer ownership.");
        Membership target = memberships.findById(targetId).filter(m -> m.getTenant().getId().equals(tid) && m.getStatus() == MembershipStatus.ACTIVE).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Member not found."));
        if (target.getId().equals(actor.getId()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OWNER_TRANSFER", "Choose another active member.");
        target.setRole(Role.OWNER);
        lockedActor.setRole(Role.ADMIN);
        audit.record(tid, current.id(), "OWNERSHIP_TRANSFERRED", "MEMBERSHIP", targetId, null);
    }

    private Membership memberInTenant(String id, String tid) {
        return memberships.findById(id).filter(m -> m.getTenant().getId().equals(tid)).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Member not found."));
    }

    private void assertAssignable(Membership actor, Role role) {
        if (role == Role.OWNER)
            throw new ApiException(HttpStatus.BAD_REQUEST, "OWNER_TRANSFER_REQUIRED", "Use ownership transfer.");
        if (actor.getRole() != Role.OWNER && (role == Role.ADMIN))
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_ESCALATION", "Only an owner can assign administrators.");
    }

    private void assertCanManage(Membership actor, Membership target, Role role) {
        assertAssignable(actor, role);
        if (target.getRole() == Role.OWNER)
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_TRANSFER_REQUIRED", "Use ownership transfer.");
        if (actor.getRole() != Role.OWNER && target.getRole() == Role.ADMIN)
            throw new ApiException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Only an owner can manage administrators.");
        if (target.getUser().getId().equals(current.id()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_MANAGEMENT_DENIED", "Ask another administrator to change your membership.");
    }

    private void protectFinalOwner(Membership target) {
        if (memberships.countByTenantIdAndRoleAndStatus(target.getTenant().getId(), Role.OWNER, MembershipStatus.ACTIVE) <= 1)
            throw new ApiException(HttpStatus.CONFLICT, "FINAL_OWNER_REQUIRED", "Transfer ownership before changing the final owner.");
    }
}
