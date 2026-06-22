package africa.credresearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * CredResearch — core backend (modular monolith).
 * Component scanning rooted at {@code africa.credresearch} picks up every feature module
 * under {@code modules.*} and the shared kernel under {@code common.*}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CredResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CredResearchApplication.class, args);
    }
}
