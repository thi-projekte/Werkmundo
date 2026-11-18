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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(PostgresDocumentTestProfile.class)
@EnabledIfEnvironmentVariable(named = "DOCUMENT_DB_TESTS", matches = "true")
class PostgresInvoiceRepositoryTest {

    private final String owner = "invoice-test-" + UUID.randomUUID();

    @Inject
    Instance<InvoiceRepository> repositories;

    @Inject
    AgroalDataSource dataSource;

    private InvoiceRepository repository() {
        InvoiceRepository repository = repositories.get();

        assertInstanceOf(PostgresInvoiceRepository.class, repository);

        return repository;
    }

    @AfterEach
    void removeOnlyThisTestsInvoices() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "DELETE FROM invoices WHERE owner_id = ?")) {

            statement.setString(1, owner);
            statement.executeUpdate();
        }
    }

    @Test
    void storesAndReloadsInvoiceWithOrderedPositions() {
        // Absichtlich unsortiert: Das Repository soll nach Reihenfolge laden.
        Invoice invoice = invoice(List.of(
                position(2, "Zweite Position", "2.0000"),
                position(1, "Erste Position", "2.0000")
        ));

        repository().insert(invoice);

        Invoice loaded = repository()
                .findById(invoice.id(), owner)
                .orElseThrow();

        assertEquals(invoice.id(), loaded.id());
        assertEquals(owner, loaded.ownerId());
        assertEquals(invoice.offerBusinessKey(), loaded.offerBusinessKey());
        assertNull(loaded.rechnungsnummer());
        assertEquals(invoice.kundendaten(), loaded.kundendaten());
        assertEquals(invoice.gesamtPreis(), loaded.gesamtPreis());
        assertEquals(invoice.createdAt(), loaded.createdAt());
        assertEquals(invoice.updatedAt(), loaded.updatedAt());
        assertEquals(
                List.of(invoice.positions().get(1), invoice.positions().get(0)),
                loaded.positions()
        );

        assertEquals(
                List.of(invoice.id()),
                repository().findByOwner(owner).stream()
                        .map(Invoice::id)
                        .toList()
        );
    }

    @Test
    void neverReturnsAnotherOwnersInvoice() {
        Invoice invoice = invoice(List.of(
                position(1, "Material", "2.0000")
        ));
        repository().insert(invoice);

        String otherOwner = "other-" + UUID.randomUUID();

        assertTrue(repository().findById(invoice.id(), otherOwner).isEmpty());
        assertTrue(repository().findByOwner(otherOwner).isEmpty());
        assertTrue(repository().findById(UUID.randomUUID(), owner).isEmpty());
        assertEquals(1, repository().findByOwner(owner).size());
    }

    @Test
    void rollsBackInvoiceWhenOnePositionIsInvalid() {
        Invoice invoice = invoice(List.of(
                position(1, "Gueltige Position", "2.0000"),
                position(2, "Ungueltige Position", "-1.0000")
        ));

        // Die negative Menge verletzt den CHECK-Constraint in PostgreSQL.
        assertThrows(
                RuntimeException.class,
                () -> repository().insert(invoice)
        );

        // Auch der zuvor eingefuegte Rechnungskopf muss zurueckgerollt sein.
        assertTrue(repository().findById(invoice.id(), owner).isEmpty());
        assertTrue(repository().findByOwner(owner).isEmpty());
    }

    private Invoice invoice(List<InvoicePosition> positions) {
        Instant timestamp = Instant.parse("2026-11-18T12:00:00Z");

        BigDecimal total = positions.stream()
                .map(InvoicePosition::positionsPreis)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);

        return new Invoice(
                UUID.randomUUID(),
                owner,
                "offer-" + UUID.randomUUID(),
                null,
                new InvoiceCustomerSnapshot(
                        "Max",
                        "Mustermann",
                        "max@example.com",
                        "Musterstrasse",
                        "12",
                        "85049",
                        "Ingolstadt"
                ),
                total,
                timestamp,
                timestamp,
                positions
        );
    }

    private InvoicePosition position(
            int sequence, String description, String quantity) {

        return new InvoicePosition(
                "Testhersteller",
                description,
                new BigDecimal(quantity),
                "Stk.",
                "catalog-test-product",
                new BigDecimal("12.50"),
                new BigDecimal("25.00"),
                sequence,
                InvoicePositionType.MATERIAL
        );
    }
}