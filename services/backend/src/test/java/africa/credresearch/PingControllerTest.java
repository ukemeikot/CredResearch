package africa.credresearch;

import static org.assertj.core.api.Assertions.assertThat;

import africa.credresearch.common.web.PingController;
import org.junit.jupiter.api.Test;

/** Plain unit test for the Phase-0 ping endpoint (no Spring context needed). */
class PingControllerTest {

    @Test
    void pingReturnsOk() {
        var body = new PingController().ping();
        assertThat(body).containsEntry("status", "ok").containsEntry("service", "backend");
    }
}
