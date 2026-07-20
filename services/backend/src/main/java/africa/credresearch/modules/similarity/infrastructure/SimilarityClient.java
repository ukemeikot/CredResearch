package africa.credresearch.modules.similarity.infrastructure;

import africa.credresearch.common.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls the worker's internal similarity checker (Phase 9, FR-SIM). Deterministic; no LLM. */
@Component
public class SimilarityClient {

    private final RestClient client;
    private final String internalSecret;

    public SimilarityClient(@Value("${credresearch.ai.worker-url}") String workerUrl,
                            @Value("${credresearch.ai.internal-secret:}") String internalSecret) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(workerUrl)
                .build();
        this.internalSecret = internalSecret;
    }

    public JsonNode check(JsonNode body) {
        try {
            return client.post()
                    .uri("/similarity/check")
                    .header("X-Internal-Secret", internalSecret)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(SimilarityClient.class).warn("Similarity call failed: {}", e.toString());
            throw ApiException.serviceUnavailable("SIMILARITY_UNAVAILABLE",
                    "The similarity service is temporarily unavailable. Please try again shortly.");
        }
    }
}
