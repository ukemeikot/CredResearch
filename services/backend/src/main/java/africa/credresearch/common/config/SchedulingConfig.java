package africa.credresearch.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables Spring's {@code @Scheduled} support (e.g. milestone-due reminders, FR-PROJ-5). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
