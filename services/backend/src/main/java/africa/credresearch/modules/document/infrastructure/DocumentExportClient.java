package africa.credresearch.modules.document.infrastructure;

import africa.credresearch.common.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the private FastAPI worker's document-export endpoints (DOCX/PDF rendering) with the shared
 * internal secret. Separate from the AI client because it returns binary payloads (FR-DOC-6/7).
 */
@Component
public class DocumentExportClient {

    private final RestClient client;
    private final String internalSecret;

    public DocumentExportClient(@Value("${credresearch.ai.worker-url}") String workerUrl,
                                @Value("${credresearch.ai.internal-secret:}") String internalSecret) {
        // Generous read timeout: LibreOffice PDF conversion of a large document can take a while.
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(180));
        this.client = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(workerUrl)
                .build();
        this.internalSecret = internalSecret;
    }

    /** POSTs {@code body} to {@code /export/<format>} and returns the rendered file bytes. */
    public byte[] render(String format, JsonNode body) {
        try {
            return client.post()
                    .uri("/export/{format}", format)
                    .header("X-Internal-Secret", internalSecret)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(DocumentExportClient.class)
                    .warn("Export worker call to /export/{} failed: {}", format, e.toString());
            throw ApiException.serviceUnavailable("EXPORT_UNAVAILABLE",
                    "The export service is temporarily unavailable. Please try again shortly.");
        }
    }
}
