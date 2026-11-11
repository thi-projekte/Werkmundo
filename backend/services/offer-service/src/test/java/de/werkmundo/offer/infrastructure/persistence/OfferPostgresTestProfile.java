package de.werkmundo.offer.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.HashMap;
import java.util.Map;

public class OfferPostgresTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        String password = System.getenv("OFFER_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("OFFER_DB_PASSWORD is required for PostgreSQL tests");
        }
        Map<String, String> config = new HashMap<>();
        config.put("offer.persistence", "postgres");
        config.put("quarkus.datasource.db-kind", "postgresql");
        config.put("quarkus.datasource.jdbc.driver", "org.postgresql.Driver");
        config.put("quarkus.datasource.jdbc.url", env("OFFER_DB_JDBC_URL", "jdbc:postgresql://127.0.0.1:55432/offer_db"));
        config.put("quarkus.datasource.username", env("OFFER_DB_USER", "offer_user"));
        config.put("quarkus.datasource.password", password);
        config.put("quarkus.hibernate-orm.active", "true");
        config.put("quarkus.hibernate-orm.schema-management.strategy", "validate");
        config.put("quarkus.hibernate-orm.sql-load-script", "no-file");
        config.put("quarkus.flyway.migrate-at-start", "true");
        config.put("quarkus.oidc.tenant-enabled", "false");
        return config;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}