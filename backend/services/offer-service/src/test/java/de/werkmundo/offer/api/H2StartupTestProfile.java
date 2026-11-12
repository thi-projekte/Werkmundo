package de.werkmundo.offer.api;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class H2StartupTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.datasource.db-kind", "h2",
                "quarkus.datasource.jdbc.driver", "org.h2.Driver",
                "quarkus.datasource.jdbc.url", "jdbc:h2:mem:startup;DB_CLOSE_DELAY=-1",
                "quarkus.datasource.username", "sa",
                "quarkus.datasource.password", "sa",
                "quarkus.flyway.migrate-at-start", "false",
                "quarkus.hibernate-orm.active", "true",
                "quarkus.hibernate-orm.schema-management.strategy", "drop-and-create",
                "offer.persistence", "memory");
    }
}