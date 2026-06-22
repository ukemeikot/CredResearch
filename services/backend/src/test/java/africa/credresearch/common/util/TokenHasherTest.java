package africa.credresearch.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void sha256IsDeterministic() {
        assertThat(TokenHasher.sha256("abc")).isEqualTo(TokenHasher.sha256("abc"));
    }

    @Test
    void sha256DiffersForDifferentInput() {
        assertThat(TokenHasher.sha256("abc")).isNotEqualTo(TokenHasher.sha256("abd"));
    }

    @Test
    void randomTokenIsHighEntropyAndUnique() {
        String a = TokenHasher.randomToken();
        String b = TokenHasher.randomToken();
        assertThat(a).isNotEqualTo(b);
        assertThat(a.length()).isGreaterThanOrEqualTo(40); // 32 bytes base64url ≈ 43 chars
    }
}
