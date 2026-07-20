package africa.credresearch.modules.similarity.interfaces.rest;

import africa.credresearch.modules.similarity.application.SimilarityService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/similarity")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Similarity", description = "Internal similarity pre-check vs your own documents "
        + "(Phase 9, FR-SIM). Not a Turnitin-equivalent originality service.")
public class SimilarityController {

    private final SimilarityService service;

    public SimilarityController(SimilarityService service) {
        this.service = service;
    }

    @PostMapping("/documents/{id}")
    @Operation(summary = "Run an internal similarity check on a document (FR-SIM-1/3/4/5)")
    public JsonNode check(@PathVariable UUID id) {
        return service.check(id);
    }
}
