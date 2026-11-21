package de.werkmundo.document.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentRepository;
import de.werkmundo.document.domain.DocumentType;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Explicit opt-in prevents a default test run from needing an external database. */
@QuarkusTest
@TestProfile(PostgresDocumentTestProfile.class)
@EnabledIfEnvironmentVariable(named = "DOCUMENT_DB_TESTS", matches = "true")
class PostgresDocumentRepositoryTest {
    private final String owner = "repository-test-" + UUID.randomUUID();
    private static final byte[] CONTENT = {37, 80, 68, 70, 0, -1, 13, 10};

    @Inject
    Instance<DocumentRepository> repositories;

    @Inject
    AgroalDataSource dataSource;

    private DocumentRepository repository() {
        DocumentRepository repository = repositories.get();
        assertInstanceOf(PostgresDocumentRepository.class, repository,
                "This test must exercise PostgreSQL, not the API test's in-memory adapter");
        return repository;
    }

    @AfterEach
    void removeOnlyThisTestsDocuments() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("DELETE FROM documents WHERE owner_id = ?")) {
            statement.setString(1, owner);
            statement.executeUpdate();
        }
    }

    @Test
    void storesAndReloadsMetadataAndBinaryContent() {
        String reference = "invoice-" + UUID.randomUUID();
        String id = repository().save(new Document(DocumentType.INVOICE, reference, owner, "customer-1",
                "customer@example.com", "invoice.pdf", CONTENT));

        // The repository opens a fresh JDBC connection for this query.
        Document loaded = repository().findByIdAndOwner(id, owner).orElseThrow();
        assertEquals(DocumentType.INVOICE, loaded.type());
        assertEquals(reference, loaded.referenceId());
        assertEquals(owner, loaded.ownerId());
        assertEquals("customer-1", loaded.customerId());
        assertEquals("customer@example.com", loaded.recipientEmail());
        assertEquals("invoice.pdf", loaded.fileName());
        assertArrayEquals(CONTENT, loaded.pdfContent());
        var metadata = repository().findMetadataByIdAndOwner(id, owner).orElseThrow();
        assertEquals(id, metadata.documentId());
        assertEquals(reference, metadata.referenceId());
        assertEquals(DocumentType.INVOICE, metadata.type());
        assertEquals("invoice.pdf", metadata.fileName());
    }

    @Test
    void neverReturnsAnotherOwnersDocuments() {
        String reference = "offer-" + UUID.randomUUID();
        String id = repository().save(new Document(DocumentType.OFFER, reference, owner, null,
                "customer@example.com", "offer.pdf", CONTENT));
        String otherOwner = "other-" + UUID.randomUUID();

        assertTrue(repository().findByIdAndOwner(id, otherOwner).isEmpty());
        assertTrue(repository().findMetadataByIdAndOwner(id, otherOwner).isEmpty());
        assertTrue(repository().findLatestMetadataByReferenceAndOwner(
                DocumentType.OFFER, reference, otherOwner).isEmpty());
        assertTrue(repository().findMetadataByOwner(otherOwner).isEmpty());
        assertEquals(1, repository().findMetadataByOwner(owner).size());
    }

    @Test
    void findsLatestVersionWithoutMixingDocumentTypes() {
        String reference = "reference-" + UUID.randomUUID();
        repository().save(new Document(DocumentType.OFFER, reference, owner, null, null, "old.pdf", CONTENT));
        String latest = repository().save(new Document(
                DocumentType.OFFER, reference, owner, null, null, "new.pdf", CONTENT));
        String invoice = repository().save(new Document(
                DocumentType.INVOICE, reference, owner, null, null, "invoice.pdf", CONTENT));

        assertEquals(latest, repository().findLatestMetadataByReferenceAndOwner(
                DocumentType.OFFER, reference, owner).orElseThrow().documentId());
        assertEquals(invoice, repository().findLatestMetadataByReferenceAndOwner(
                DocumentType.INVOICE, reference, owner).orElseThrow().documentId());
        assertTrue(repository().findByIdAndOwner("missing", owner).isEmpty());
    }

    @Test
    void flywayHasAppliedTheDocumentSchema() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true");
             var result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }
}
