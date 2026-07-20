package africa.credresearch.modules.analysis.infrastructure;

import africa.credresearch.common.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Sends CSV bytes to the worker for descriptive analysis (Phase 8, FR-DATA). */
@Component
public class AnalysisClient {

    private final RestClient client;
    private final String internalSecret;

    public AnalysisClient(@Value("${credresearch.ai.worker-url}") String workerUrl,
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

    public JsonNode describe(byte[] csv) {
        try {
            return client.post()
                    .uri("/analysis/describe")
                    .header("X-Internal-Secret", internalSecret)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(csv)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(AnalysisClient.class).warn("Analysis call failed: {}", e.toString());
            throw ApiException.serviceUnavailable("ANALYSIS_UNAVAILABLE",
                    "The analysis service is temporarily unavailable. Please try again shortly.");
        }
    }
}
