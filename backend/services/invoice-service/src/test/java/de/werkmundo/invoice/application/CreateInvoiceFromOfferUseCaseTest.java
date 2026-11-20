package de.werkmundo.invoice.application;

import de.werkmundo.invoice.document.infrastructure.PostgresDocumentTestProfile;
import de.werkmundo.invoice.domain.InvoiceRepository;
import de.werkmundo.invoice.infrastructure.offer.InvoiceOfferLoader;
import de.werkmundo.invoice.infrastructure.offer.OfferPositionResponse;
import de.werkmundo.invoice.infrastructure.offer.OfferResponse;
import de.werkmundo.invoice.infrastructure.offer.OfferServiceClient;
import de.werkmundo.invoice.infrastructure.user.CustomerResponse;
import de.werkmundo.invoice.infrastructure.user.UserServiceClient;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(PostgresDocumentTestProfile.class)
@EnabledIfEnvironmentVariable(named = "DOCUMENT_DB_TESTS", matches = "true")
class CreateInvoiceFromOfferUseCaseTest {

    private static final String AUTHORIZATION = "Bearer test-token";

    private final String owner = "invoice-flow-test-" + UUID.randomUUID();
    private final String businessKey = "offer-" + UUID.randomUUID();

    @Inject
    CreateInvoiceDraftUseCase createDraft;

    @Inject
    Instance<InvoiceRepository> repositories;

    @Inject
    AgroalDataSource dataSource;

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
    void createsInvoiceFromOwnAcceptedOfferAndCustomer() {
        OfferServiceClient offerClient = (key, authorization) -> {
            assertEquals(businessKey, key);
            assertEquals(AUTHORIZATION, authorization);
            return offer(owner);
        };

        UserServiceClient userClient = (id, authorization) -> {
            assertEquals(Long.valueOf(42), id);
            assertEquals(AUTHORIZATION, authorization);
            return customer(42L);
        };

        var useCase = useCase(offerClient, userClient);

        var created = useCase.create(businessKey, owner, AUTHORIZATION);
        var loaded = repositories.get()
                .findById(created.id(), owner)
                .orElseThrow();

        assertEquals(businessKey, loaded.offerBusinessKey());
        assertEquals("Max", loaded.kundendaten().vorname());
        assertEquals("max@example.com", loaded.kundendaten().email());
        assertEquals(new BigDecimal("25.00"), loaded.gesamtPreis());
        assertEquals(1, loaded.positions().size());
        assertEquals(new BigDecimal("25.00"),
                loaded.positions().getFirst().positionsPreis());
        assertNull(loaded.rechnungsnummer());
    }

    @Test
    void rejectsForeignOfferBeforeLoadingCustomerOrSaving() {
        OfferServiceClient offerClient =
                (key, authorization) -> offer("other-owner");

        UserServiceClient userClient = (id, authorization) -> {
            fail("Bei fremdem Angebot darf kein Kundenabruf erfolgen");
            return null;
        };

        var useCase = useCase(offerClient, userClient);

        assertThrows(ForbiddenException.class,
                () -> useCase.create(businessKey, owner, AUTHORIZATION));

        assertTrue(repositories.get().findByOwner(owner).isEmpty());
    }

    @Test
    void rejectsMismatchedCustomerWithoutSaving() {
        OfferServiceClient offerClient =
                (key, authorization) -> offer(owner);

        UserServiceClient userClient =
                (id, authorization) -> customer(99L);

        var useCase = useCase(offerClient, userClient);

        WebApplicationException exception = assertThrows(
                WebApplicationException.class,
                () -> useCase.create(businessKey, owner, AUTHORIZATION));

        assertEquals(502, exception.getResponse().getStatus());
        assertTrue(repositories.get().findByOwner(owner).isEmpty());
    }

    @Test
    void repeatedRequestReturnsSavedInvoiceWithoutCallingRemoteServices() {
        UUID key = UUID.randomUUID();

        var initialUseCase = useCase(
                (requestedKey, authorization) -> offer(owner),
                (customerId, authorization) -> customer(42L)
        );

        var first = initialUseCase.create(
                businessKey, owner, AUTHORIZATION, key);

        OfferServiceClient unavailableOfferClient = (requestedKey, authorization) -> {
            fail("Bei Wiederholung darf kein Angebotsabruf erfolgen");
            return null;
        };

        UserServiceClient unavailableUserClient = (customerId, authorization) -> {
            fail("Bei Wiederholung darf kein Kundenabruf erfolgen");
            return null;
        };

        var repeatedUseCase = useCase(
                unavailableOfferClient, unavailableUserClient);

        var repeated = repeatedUseCase.create(
                businessKey, owner, AUTHORIZATION, key);

        assertEquals(first.id(), repeated.id());
        assertEquals(first.positions(), repeated.positions());
        assertEquals(1, repositories.get().findByOwner(owner).size());
    }

    private CreateInvoiceFromOfferUseCase useCase(
        OfferServiceClient offerClient,
        UserServiceClient userClient) {

    return new CreateInvoiceFromOfferUseCase(
            new InvoiceOfferLoader(offerClient),
            userClient,
            createDraft,
            repositories
        );
    }


    private OfferResponse offer(String offerOwner) {
        return new OfferResponse(
                businessKey,
                "ANGENOMMEN",
                "42",
                offerOwner,
                List.of(new OfferPositionResponse(
                        "Testhersteller",
                        "Material",
                        new BigDecimal("2"),
                        "Stk.",
                        "catalog-test",
                        new BigDecimal("12.50"),
                        1,
                        "MATERIAL"
                ))
        );
    }

    private CustomerResponse customer(Long id) {
        return new CustomerResponse(
                id,
                "max@example.com",
                "Max",
                "Mustermann",
                "Musterstrasse",
                "12",
                "85049",
                "Ingolstadt"
        );
    }
}