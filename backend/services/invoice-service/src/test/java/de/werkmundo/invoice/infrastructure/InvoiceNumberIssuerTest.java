package de.werkmundo.invoice.infrastructure;

import de.werkmundo.invoice.document.infrastructure.PostgresDocumentTestProfile;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(PostgresDocumentTestProfile.class)
@EnabledIfEnvironmentVariable(named = "DOCUMENT_DB_TESTS", matches = "true")
class InvoiceNumberIssuerTest {
    @Inject AgroalDataSource dataSource;
    @Inject InvoiceNumberIssuer issuer;

    @Test
    void issuingInvoiceIsPersistentAndIdempotent() throws Exception {
        UUID id = UUID.randomUUID();
        String owner = "issue18-" + UUID.randomUUID();
        try {
            try (var connection = dataSource.getConnection();
                 var insert = connection.prepareStatement(
                     "INSERT INTO invoices (id, owner_id, offer_business_key, gesamt_preis, created_at, updated_at) VALUES (?, ?, ?, 1.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                insert.setObject(1, id);
                insert.setString(2, owner);
                insert.setString(3, "issue18-test-offer");
                assertEquals(1, insert.executeUpdate());
            }

            String first = issuer.issue(id, owner).orElseThrow();
            assertTrue(first.matches("^RE-[0-9]{6}-[0-9]{4}$"));
            assertEquals(first, issuer.issue(id, owner).orElseThrow());
            assertTrue(issuer.issue(id, "other-owner").isEmpty());

            try (var connection = dataSource.getConnection();
                 var select = connection.prepareStatement(
                     "SELECT rechnungsnummer FROM invoices WHERE id = ?")) {
                select.setObject(1, id);
                try (var result = select.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(first, result.getString(1));
                }
            }
        } finally {
            try (var connection = dataSource.getConnection();
                 var delete = connection.prepareStatement("DELETE FROM invoices WHERE id = ?")) {
                delete.setObject(1, id);
                delete.executeUpdate();
            }
        }
    }
}
