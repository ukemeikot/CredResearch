package africa.credresearch.modules.analysis.interfaces.rest;

import africa.credresearch.common.error.ApiException;
import africa.credresearch.modules.analysis.application.DataAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/analysis")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Data analysis", description = "Descriptive statistics + grounded AI interpretation (Phase 8, FR-DATA).")
public class AnalysisController {

    private final DataAnalysisService service;

    public AnalysisController(DataAnalysisService service) {
        this.service = service;
    }

    public record AiRequest(UUID projectId, String topic, JsonNode stats) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a CSV and get descriptive statistics (FR-DATA-1..4)")
    public JsonNode describe(@RequestParam UUID projectId, @RequestParam("file") MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw ApiException.badRequest("UPLOAD_READ_FAILED", "Could not read the uploaded file");
        }
        return service.describe(projectId, bytes);
    }

    @PostMapping("/interpret")
    @Operation(summary = "Grounded AI interpretation of the statistics (FR-DATA-5)")
    public JsonNode interpret(@RequestBody AiRequest req) {
        return service.interpret(req.projectId(), req.topic(), req.stats());
    }

    @PostMapping("/chapter4")
    @Operation(summary = "Generate a Chapter 4 starter draft from the statistics (FR-DATA-6)")
    public JsonNode chapter4(@RequestBody AiRequest req) {
        return service.chapter4(req.projectId(), req.topic(), req.stats());
    }
}
