package africa.credresearch.modules.document.infrastructure;

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

/**
 * Sends an uploaded .docx/.pdf to the private worker to be converted into the editor's ProseMirror
 * structure (sections + content), so an existing document can be imported and edited in-app.
 */
@Component
public class DocumentImportClient {

    private final RestClient client;
    private final String internalSecret;

    public DocumentImportClient(@Value("${credresearch.ai.worker-url}") String workerUrl,
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

    /** Convert the file bytes to {title, sections:[{heading, content}]}. */
    public JsonNode convert(String filename, byte[] bytes) {
        try {
            return client.post()
                    .uri("/import/document")
                    .header("X-Internal-Secret", internalSecret)
                    .header("X-Filename", filename == null ? "upload.docx" : filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            org.slf4j.LoggerFactory.getLogger(DocumentImportClient.class)
                    .warn("Document import worker call failed for {}: {}", filename, e.toString());
            throw ApiException.badRequest("IMPORT_FAILED",
                    "Could not read that file. Please upload a valid .docx or .pdf.");
        }
    }
}
