package africa.credresearch.modules.paper.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sends an uploaded file's raw bytes to the private worker for text + metadata extraction
 * (Phase 5). On any failure it returns a low-confidence empty result rather than throwing, so an
 * upload is still recorded and the user can fill in the details manually (graceful degradation).
 */
@Component
public class PaperExtractionClient {

    private final RestClient client;
    private final String internalSecret;
    private final ObjectMapper mapper;

    public PaperExtractionClient(@Value("${credresearch.ai.worker-url}") String workerUrl,
                                 @Value("${credresearch.ai.internal-secret:}") String internalSecret,
                                 ObjectMapper mapper) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(workerUrl)
                .build();
        this.internalSecret = internalSecret;
        this.mapper = mapper;
    }

    public JsonNode extract(String filename, byte[] bytes) {
        try {
            JsonNode res = client.post()
                    .uri("/papers/extract")
                    .header("X-Internal-Secret", internalSecret)
                    .header("X-Filename", filename == null ? "upload.pdf" : filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes)
                    .retrieve()
                    .body(JsonNode.class);
            return res != null ? res : lowConfidence();
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(PaperExtractionClient.class)
                    .warn("Paper extraction failed for {}: {}", filename, e.toString());
            return lowConfidence();
        }
    }

    private JsonNode lowConfidence() {
        return mapper.createObjectNode().put("low_confidence", true);
    }
}
