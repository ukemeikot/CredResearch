package africa.credresearch.modules.project.application;

import africa.credresearch.common.config.CredResearchProperties;
import africa.credresearch.common.error.ApiException;
import africa.credresearch.common.notification.NotificationPort;
import africa.credresearch.common.tenant.TenantContext;
import africa.credresearch.common.tenant.TenantContextHolder;
import africa.credresearch.common.util.TokenHasher;
import africa.credresearch.modules.identity.domain.model.User;
import africa.credresearch.modules.identity.domain.port.UserRepository;
import africa.credresearch.modules.project.domain.InvitationStatus;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.model.Invitation;
import africa.credresearch.modules.project.domain.model.Project;
import africa.credresearch.modules.project.domain.model.ProjectMember;
import africa.credresearch.modules.project.domain.port.InvitationRepository;
import africa.credresearch.modules.project.domain.port.ProjectMemberRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email invitations to join a project (FR-PROJ-3). Adding a member is an email invite, not a raw
 * user id: the invitee accepts via a tokenized link. Acceptance requires the invitee to belong to
 * the same institution as the project (intra-tenant collaboration), preserving tenant isolation.
 */
@Service
public class InvitationService {

    private static final Duration INVITE_TTL = Duration.ofDays(14);

    private final InvitationRepository invitations;
    private final ProjectMemberRepository members;
    private final ProjectAccessGuard accessGuard;
    private final ActivityService activityService;
    private final UserRepository users;
    private final NotificationPort notifications;
    private final CredResearchProperties props;

    public InvitationService(InvitationRepository invitations, ProjectMemberRepository members,
                             ProjectAccessGuard accessGuard, ActivityService activityService,
                             UserRepository users, NotificationPort notifications,
                             CredResearchProperties props) {
        this.invitations = invitations;
        this.members = members;
        this.accessGuard = accessGuard;
        this.activityService = activityService;
        this.users = users;
        this.notifications = notifications;
        this.props = props;
    }

    /** Creates a pending invite and emails a tokenized accept link. Requires OWNER. */
    @Transactional
    public Invitation invite(UUID projectId, String rawEmail, ProjectMemberRole role) {
        TenantContext ctx = TenantContextHolder.require();
        Project project = accessGuard.requireRole(projectId, ProjectMemberRole.OWNER);
        if (role == ProjectMemberRole.OWNER) {
            throw ApiException.badRequest("INVALID_ROLE", "A project can only have one owner");
        }
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (email.isBlank()) {
            throw ApiException.badRequest("INVALID_EMAIL", "An email address is required");
        }
        users.findByEmail(email).ifPresent(u -> {
            if (members.existsByProjectAndUser(projectId, u.id())) {
                throw ApiException.conflict("ALREADY_MEMBER", "That person is already a project member");
            }
        });
        // A live pending invite blocks a duplicate; an expired one is retired so re-inviting works.
        invitations.findPendingByProjectAndEmail(projectId, email).ifPresent(existing -> {
            if (existing.expiresAt().isAfter(Instant.now())) {
                throw ApiException.conflict("INVITE_PENDING", "An invitation is already pending for that email");
            }
            invitations.markExpired(existing.id());
        });

        String raw = TokenHasher.randomToken();
        Instant expiresAt = Instant.now().plus(INVITE_TTL);
        Invitation created = invitations.create(
                new Invitation(null, project.institutionId(), projectId, email, role.name(),
                        InvitationStatus.PENDING, expiresAt, null, null),
                TokenHasher.sha256(raw), ctx.userId());

        String link = props.app().baseUrl() + "/invite/accept?token=" + raw;
        notifications.sendEmail(email, "You've been invited to a CredResearch project",
                inviteHtml(project.title(), role.name(), link));
        activityService.record(projectId, ctx.userId(), "MEMBER_INVITED",
                Map.of("email", email, "role", role.name()));
        return created;
    }

    public List<Invitation> listPending(UUID projectId) {
        accessGuard.requireMember(projectId);
        return invitations.findPendingByProject(projectId);
    }

    @Transactional
    public void revoke(UUID projectId, UUID invitationId) {
        accessGuard.requireRole(projectId, ProjectMemberRole.OWNER);
        Invitation inv = invitations.findByIdAndProject(invitationId, projectId)
                .orElseThrow(() -> ApiException.notFound("INVITE_NOT_FOUND", "Invitation not found"));
        if (inv.status() != InvitationStatus.PENDING) {
            throw ApiException.badRequest("INVITE_NOT_PENDING", "Only a pending invitation can be revoked");
        }
        invitations.markRevoked(inv.id());
    }

    /** Accepts an invitation for the authenticated caller; adds them to the project. Returns the project id. */
    @Transactional
    public UUID accept(String rawToken) {
        TenantContext ctx = TenantContextHolder.require();
        Invitation inv = invitations.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> ApiException.badRequest("INVALID_INVITE", "This invitation link is invalid"));
        if (inv.status() != InvitationStatus.PENDING) {
            throw ApiException.badRequest("INVITE_NOT_PENDING", "This invitation is no longer active");
        }
        if (inv.expiresAt().isBefore(Instant.now())) {
            invitations.markExpired(inv.id());
            throw ApiException.badRequest("INVITE_EXPIRED", "This invitation has expired");
        }
        User caller = users.findById(ctx.userId())
                .orElseThrow(() -> ApiException.unauthorized("NO_USER", "Not authenticated"));
        if (!caller.email().equalsIgnoreCase(inv.email())) {
            throw ApiException.forbidden("INVITE_EMAIL_MISMATCH",
                    "This invitation was sent to a different email address. Sign in as " + inv.email() + ".");
        }
        if (caller.institutionId() == null || !caller.institutionId().equals(inv.institutionId())) {
            throw ApiException.forbidden("INVITE_CROSS_TENANT",
                    "This project belongs to another institution. Ask an admin to add you to that institution first.");
        }
        if (!members.existsByProjectAndUser(inv.projectId(), caller.id())) {
            members.add(new ProjectMember(null, inv.projectId(), caller.id(),
                    ProjectMemberRole.valueOf(inv.roleCode())));
            activityService.record(inv.projectId(), caller.id(), "MEMBER_JOINED",
                    Map.of("role", inv.roleCode()));
        }
        invitations.markAccepted(inv.id(), caller.id());
        return inv.projectId();
    }

    private static String inviteHtml(String projectTitle, String role, String link) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;background:#f4f6fb;padding:32px">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px;border:1px solid #e5e9f2">
                    <h1 style="margin:0 0 12px;color:#0b1026;font-size:20px">CredResearch</h1>
                    <p style="color:#334155;font-size:15px;margin:0 0 6px">You've been invited to collaborate on</p>
                    <p style="color:#0b1026;font-size:17px;font-weight:bold;margin:0 0 6px">%s</p>
                    <p style="color:#334155;font-size:14px;margin:0">as a <strong>%s</strong>.</p>
                    <p style="text-align:center;margin:28px 0">
                      <a href="%s" style="background:#2563eb;color:#ffffff;text-decoration:none;padding:13px 32px;border-radius:8px;display:inline-block;font-weight:bold;font-size:15px">Accept invitation</a>
                    </p>
                    <p style="color:#94a3b8;font-size:12px;line-height:1.5;margin:0">Sign in (or create an account with this email) to accept. This link expires in 14 days.</p>
                  </div>
                </div>
                """.formatted(escapeHtml(projectTitle), escapeHtml(role), link);
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
