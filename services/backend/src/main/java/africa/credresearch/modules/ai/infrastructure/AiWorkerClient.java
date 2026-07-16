package africa.credresearch.modules.ai.infrastructure;

import africa.credresearch.common.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the private FastAPI AI worker with the shared internal secret. The worker is never exposed
 * publicly; the backend is the only caller (FR-X / security). Requests/responses are passed through
 * as JSON — the worker owns the AI schemas and validation.
 */
@Component
public class AiWorkerClient {

    private final RestClient client;
    private final String internalSecret;

    public AiWorkerClient(@Value("${credresearch.ai.worker-url}") String workerUrl,
                          @Value("${credresearch.ai.internal-secret:}") String internalSecret) {
        // Generous read timeout: self-hosted CPU inference can take tens of seconds, and a valid
        // slow generation must not be turned into a spurious 503.
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(180));
        this.client = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(workerUrl)
                .build();
        this.internalSecret = internalSecret;
    }

    /** POSTs {@code body} to {@code /ai/<path>} on the worker and returns its JSON response. */
    public JsonNode post(String path, JsonNode body) {
        try {
            return client.post()
                    .uri("/ai/{path}", path)
                    .header("X-Internal-Secret", internalSecret)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(AiWorkerClient.class)
                    .warn("AI worker call to /ai/{} failed: {}", path, e.toString());
            throw ApiException.serviceUnavailable("AI_UNAVAILABLE",
                    "The AI service is temporarily unavailable. Please try again shortly.");
        }
    }
}
