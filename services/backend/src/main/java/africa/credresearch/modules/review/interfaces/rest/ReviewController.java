package africa.credresearch.modules.review.interfaces.rest;

import africa.credresearch.modules.review.application.ReviewService;
import africa.credresearch.modules.review.application.ReviewService.ReviewThread;
import africa.credresearch.modules.review.domain.model.ReviewComment;
import africa.credresearch.modules.review.domain.model.ReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Reviews", description = "Supervisor review loop: submit → comment → decide → revise (Phase 6, FR-SUP-3..8).")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    public record SubmitRequest(@NotNull UUID documentId, UUID documentSectionId,
                                @NotNull UUID reviewerUserId, String note) {}

    public record SubmitExternalRequest(@NotNull UUID documentId, UUID documentSectionId,
                                        @NotNull String email, String note) {}

    public record CommentRequest(String body, Integer anchorStart, Integer anchorEnd, String quote) {}

    public record ResolveRequest(boolean resolved) {}

    public record DecisionRequest(String decision, String summary) {}

    public record ResubmitRequest(String note) {}

    @PostMapping
    @Operation(summary = "Submit a section/document for review (FR-SUP-3)")
    public ReviewRequest submit(@RequestBody SubmitRequest req) {
        return service.submit(req.documentId(), req.documentSectionId(), req.reviewerUserId(), req.note());
    }

    @PostMapping("/external")
    @Operation(summary = "Invite an external supervisor by email (magic-link, no account) — FR-SUP-1/2")
    public ReviewRequest submitExternal(@RequestBody SubmitExternalRequest req) {
        return service.submitExternal(req.documentId(), req.documentSectionId(), req.email(), req.note());
    }

    @GetMapping
    @Operation(summary = "List review requests for a document with comments + decision history")
    public List<ReviewThread> list(@RequestParam UUID documentId) {
        return service.listForDocument(documentId);
    }

    @GetMapping("/inbox")
    @Operation(summary = "The caller's pending review queue (FR-SUP-7)")
    public List<ReviewRequest> inbox() {
        return service.inbox();
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add an inline comment (FR-SUP-4)")
    public ReviewComment comment(@PathVariable UUID id, @RequestBody CommentRequest req) {
        return service.comment(id, req.body(), req.anchorStart(), req.anchorEnd(), req.quote());
    }

    @PatchMapping("/comments/{commentId}")
    @Operation(summary = "Resolve / unresolve a comment thread (FR-SUP-6)")
    public ReviewComment resolve(@PathVariable UUID commentId, @RequestBody ResolveRequest req) {
        return service.resolveComment(commentId, req.resolved());
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Record a decision: APPROVED / NEEDS_REVISION / REJECTED (FR-SUP-5)")
    public ReviewThread decide(@PathVariable UUID id, @RequestBody DecisionRequest req) {
        return service.decide(id, req.decision(), req.summary());
    }

    @PostMapping("/{id}/resubmit")
    @Operation(summary = "Resubmit after revision; prior decisions preserved (FR-SUP-8)")
    public ReviewRequest resubmit(@PathVariable UUID id, @RequestBody(required = false) ResubmitRequest req) {
        return service.resubmit(id, req == null ? null : req.note());
    }
}
