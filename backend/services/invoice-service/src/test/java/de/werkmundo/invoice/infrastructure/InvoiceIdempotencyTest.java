package de.werkmundo.invoice.infrastructure;

import de.werkmundo.document.infrastructure.PostgresDocumentTestProfile;
import de.werkmundo.invoice.domain.Invoice;
import de.werkmundo.invoice.domain.InvoiceCustomerSnapshot;
import de.werkmundo.invoice.domain.InvoicePosition;
import de.werkmundo.invoice.domain.InvoicePositionType;
import de.werkmundo.invoice.domain.InvoiceRepository;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(PostgresDocumentTestProfile.class)
@EnabledIfEnvironmentVariable(named = "DOCUMENT_DB_TESTS", matches = "true")
class InvoiceIdempotencyTest {

    private final String owner = "idempotency-test-" + UUID.randomUUID();
    private final String otherOwner = "idempotency-other-" + UUID.randomUUID();

    @Inject
    Instance<InvoiceRepository> repositories;

    @Inject
    AgroalDataSource dataSource;

    @AfterEach
    void removeOnlyThisTestsInvoices() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "DELETE FROM invoices WHERE owner_id IN (?, ?)")) {

            statement.setString(1, owner);
            statement.setString(2, otherOwner);
            statement.executeUpdate();
        }
    }

    @Test
    void returnsOriginalInvoiceOnRepeatedRequest() {
        UUID key = UUID.randomUUID();

        Invoice first = repository().insertOrGet(
                invoice(owner, "offer-1", "Material", "2.0000"), key);

        Invoice repeated = repository().insertOrGet(
                invoice(owner, "offer-1", "Geaenderte Beschreibung", "2.0000"),
                key);

        assertEquals(first.id(), repeated.id());
        assertEquals("Material",
                repeated.positions().getFirst().bezeichnung());
        assertEquals(1, repository().findByOwner(owner).size());
        assertEquals(first.id(),
                repository().findByIdempotencyKey(owner, key)
                        .orElseThrow().id());
    }

    @Test
    void rejectsKeyUsedForAnotherOffer() {
        UUID key = UUID.randomUUID();

        Invoice first = repository().insertOrGet(
                invoice(owner, "offer-1", "Material", "2.0000"), key);

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> repository().insertOrGet(
                        invoice(owner, "offer-2", "Material", "2.0000"),
                        key));

        assertEquals(409, exception.getResponse().getStatus());
        assertEquals(1, repository().findByOwner(owner).size());
        assertEquals(first.id(),
                repository().findByIdempotencyKey(owner, key)
                        .orElseThrow().id());
    }

    @Test
    void scopesKeyToOwner() {
        UUID key = UUID.randomUUID();

        Invoice first = repository().insertOrGet(
                invoice(owner, "offer-1", "Material", "2.0000"), key);

        assertTrue(repository()
                .findByIdempotencyKey(otherOwner, key).isEmpty());

        Invoice second = repository().insertOrGet(
                invoice(otherOwner, "offer-2", "Material", "2.0000"), key);

        assertNotEquals(first.id(), second.id());
        assertEquals(1, repository().findByOwner(owner).size());
        assertEquals(1, repository().findByOwner(otherOwner).size());
    }

    @Test
    void rollsBackKeyAndInvoiceWhenPositionIsInvalid() {
        UUID key = UUID.randomUUID();

        assertThrows(RuntimeException.class,
                () -> repository().insertOrGet(
                        invoice(owner, "offer-1", "Material", "-1.0000"),
                        key));

        assertTrue(repository().findByIdempotencyKey(owner, key).isEmpty());
        assertTrue(repository().findByOwner(owner).isEmpty());

        Invoice valid = repository().insertOrGet(
                invoice(owner, "offer-1", "Material", "2.0000"), key);

        assertEquals(valid.id(),
                repository().findByIdempotencyKey(owner, key)
                        .orElseThrow().id());
    }

    @Test
    void concurrentRequestsReturnTheSameInvoice() throws Exception {
        UUID key = UUID.randomUUID();
        Invoice first = invoice(owner, "offer-1", "Material", "2.0000");
        Invoice second = invoice(owner, "offer-1", "Material", "2.0000");
        InvoiceRepository repository = repository();

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var firstResult = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Teststart abgelaufen");
                }
                return repository.insertOrGet(first, key);
            });

            var secondResult = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Teststart abgelaufen");
                }
                return repository.insertOrGet(second, key);
            });

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            Invoice firstSaved = firstResult.get(30, TimeUnit.SECONDS);
            Invoice secondSaved = secondResult.get(30, TimeUnit.SECONDS);

            assertEquals(firstSaved.id(), secondSaved.id());
            assertEquals(1, repository.findByOwner(owner).size());

            Invoice loaded = repository.findByIdempotencyKey(owner, key)
                    .orElseThrow();

            assertEquals(firstSaved.id(), loaded.id());
            assertEquals(1, loaded.positions().size());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private InvoiceRepository repository() {
        InvoiceRepository repository = repositories.get();
        assertInstanceOf(PostgresInvoiceRepository.class, repository);
        return repository;
    }

    private Invoice invoice(
            String invoiceOwner,
            String offerKey,
            String description,
            String quantity) {

        Instant timestamp = Instant.parse("2026-11-18T12:00:00Z");

        return new Invoice(
                UUID.randomUUID(),
                invoiceOwner,
                offerKey,
                null,
                new InvoiceCustomerSnapshot(
                        "Max", "Mustermann", "max@example.com",
                        "Musterstrasse", "12", "85049", "Ingolstadt"
                ),
                new BigDecimal("25.00"),
                timestamp,
                timestamp,
                List.of(new InvoicePosition(
                        null,
                        description,
                        new BigDecimal(quantity),
                        "Stk.",
                        null,
                        new BigDecimal("12.50"),
                        new BigDecimal("25.00"),
                        1,
                        InvoicePositionType.MATERIAL
                ))
        );
    }
}