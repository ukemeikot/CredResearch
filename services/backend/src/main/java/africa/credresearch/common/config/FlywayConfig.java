package africa.credresearch.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs {@code repair()} before {@code migrate()} on startup.
 *
 * <p>Rolling deploys can leave the schema-history in a half-applied state if a new instance is
 * interrupted mid-migration (the object gets created but the history row isn't finalised). Plain
 * auto-migrate then crash-loops on the next boot. {@code repair()} clears failed/partial history
 * entries and realigns checksums so {@code migrate()} can proceed cleanly. Combined with the
 * idempotent-DDL convention for migrations (see db/migration), first-deploy migrations are
 * self-healing rather than requiring manual DB surgery.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return (Flyway flyway) -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
