package africa.credresearch.modules.project.interfaces.rest;

import africa.credresearch.modules.project.application.InvitationService;
import africa.credresearch.modules.project.domain.ProjectMemberRole;
import africa.credresearch.modules.project.domain.model.Invitation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Invitations", description = "Email invitations to join a project (FR-PROJ-3). Adding a "
        + "member is an email invite, not a raw user id; the invitee accepts via a tokenized link.")
public class InvitationController {

    private final InvitationService service;

    public InvitationController(InvitationService service) {
        this.service = service;
    }

    public record InviteRequest(@Email @NotBlank String email, @NotNull ProjectMemberRole role) {}

    public record AcceptRequest(@NotBlank String token) {}

    public record InvitationResponse(UUID id, String email, String role, String status, Instant expiresAt) {
        static InvitationResponse from(Invitation i) {
            return new InvitationResponse(i.id(), i.email(), i.roleCode(), i.status().name(), i.expiresAt());
        }
    }

    @PostMapping("/projects/{id}/invitations")
    @Operation(summary = "Invite a member by email", description = "Requires OWNER. Emails a tokenized accept link.")
    public InvitationResponse invite(@PathVariable UUID id, @Valid @RequestBody InviteRequest req) {
        return InvitationResponse.from(service.invite(id, req.email(), req.role()));
    }

    @GetMapping("/projects/{id}/invitations")
    @Operation(summary = "List pending invitations", description = "Member-only.")
    public List<InvitationResponse> list(@PathVariable UUID id) {
        return service.listPending(id).stream().map(InvitationResponse::from).toList();
    }

    @DeleteMapping("/projects/{id}/invitations/{invitationId}")
    @Operation(summary = "Revoke a pending invitation", description = "Requires OWNER.")
    public void revoke(@PathVariable UUID id, @PathVariable UUID invitationId) {
        service.revoke(id, invitationId);
    }

    @PostMapping("/invitations/accept")
    @Operation(summary = "Accept an invitation", description = "The signed-in invitee joins the project.")
    public Map<String, UUID> accept(@Valid @RequestBody AcceptRequest req) {
        return Map.of("projectId", service.accept(req.token()));
    }
}
