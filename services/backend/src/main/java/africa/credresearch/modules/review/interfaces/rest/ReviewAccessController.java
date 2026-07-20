package africa.credresearch.modules.review.interfaces.rest;

import africa.credresearch.modules.review.application.ReviewService;
import africa.credresearch.modules.review.application.ReviewService.ExternalView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public magic-link review surface for account-less external supervisors (Phase 6, FR-SUP-2).
 * Authorization is the scoped review token itself (opaque, hashed, 14-day expiry) — it grants
 * access to exactly one review request and cannot act anywhere else. These routes are permitted
 * without a bearer in SecurityConfig; every call self-authorizes via the token.
 */
@RestController
@RequestMapping("/api/v1/review-access")
@Tag(name = "Review (magic-link)", description = "Account-less external review via a scoped token.")
public class ReviewAccessController {

    private final ReviewService service;

    public ReviewAccessController(ReviewService service) {
        this.service = service;
    }

    public record CommentRequest(String body, Integer anchorStart, Integer anchorEnd, String quote) {}

    public record DecisionRequest(String decision, String summary) {}

    @GetMapping("/{token}")
    @Operation(summary = "View the shared section + threads for this review token")
    public ExternalView view(@PathVariable String token) {
        return service.accessByToken(token);
    }

    @PostMapping("/{token}/comments")
    @Operation(summary = "Leave a comment as the external reviewer")
    public ExternalView comment(@PathVariable String token, @RequestBody CommentRequest req) {
        return service.externalComment(token, req.body(), req.anchorStart(), req.anchorEnd(), req.quote());
    }

    @PostMapping("/{token}/decision")
    @Operation(summary = "Record a decision as the external reviewer")
    public ExternalView decide(@PathVariable String token, @RequestBody DecisionRequest req) {
        return service.externalDecide(token, req.decision(), req.summary());
    }
}
