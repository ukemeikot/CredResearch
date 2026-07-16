package africa.credresearch.modules.document.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** ProseMirror → plain-text flattening used for content_text (FTS/similarity). */
class ProseMirrorTextTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void flattensNestedTextNodesAcrossBlocks() throws Exception {
        String doc = """
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"Background"}]},
                  {"type":"paragraph","content":[{"type":"text","text":"Hello "},{"type":"text","text":"world"}]}
                ]}""";
        String text = ProseMirrorText.flatten(mapper.readTree(doc));
        assertThat(text).contains("Background");
        assertThat(text).contains("Hello world");
    }

    @Test
    void nullOrEmptyDocFlattensToEmptyString() throws Exception {
        assertThat(ProseMirrorText.flatten(null)).isEmpty();
        assertThat(ProseMirrorText.flatten(mapper.readTree("{\"type\":\"doc\",\"content\":[]}"))).isEmpty();
    }
}
