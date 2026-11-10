package de.werkmundo.document.infrastructure;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class PostgresDocumentTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("document.storage.postgres.enabled", "true");
    }
}
