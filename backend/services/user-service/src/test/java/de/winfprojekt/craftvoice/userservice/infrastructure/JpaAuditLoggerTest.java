package de.winfprojekt.craftvoice.userservice.infrastructure;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class JpaAuditLoggerTest {

    @Inject
    JpaAuditLogger auditLogger;

    @Test
    @TestTransaction
    void persistsTheProvidedAuditEventUsingTheExistingEntity() {
        auditLogger.log(31415L, "AUDIT_TEST", "Safe test description");

        AuditLogEntity entry = AuditLogEntity.find(
                "userId = ?1 and action = ?2",
                31415L,
                "AUDIT_TEST"
        ).firstResult();

        assertNotNull(entry);
        assertEquals(31415L, entry.userId);
        assertEquals("AUDIT_TEST", entry.action);
        assertEquals("Safe test description", entry.description);
    }
}
