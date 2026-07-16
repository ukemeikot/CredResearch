package africa.credresearch.modules.ai.infrastructure;

import africa.credresearch.common.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
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
        this.client = RestClient.builder().baseUrl(workerUrl).build();
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
            throw ApiException.serviceUnavailable("AI_UNAVAILABLE",
                    "The AI service is temporarily unavailable. Please try again shortly.");
        }
    }
}
