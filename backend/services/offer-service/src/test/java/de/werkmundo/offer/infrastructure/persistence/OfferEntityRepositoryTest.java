
package de.werkmundo.offer.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.TestTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OfferEntityRepositoryTest {

    @Inject
    OfferEntityRepository repository;

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void shouldSaveAndLoadOfferWithRelations() {
        OfferEntity offer = new OfferEntity();
        offer.businessKey = "test-" + UUID.randomUUID();
        offer.customerId = "customer-1";
        offer.craftsmanId = "craftsman-1";
        offer.status = "DRAFT";
        offer.createdAt = LocalDateTime.now();
        offer.updatedAt = offer.createdAt;

        OfferPositionEntity position = new OfferPositionEntity();
        position.offer = offer;
        position.type = "MATERIAL";
        position.name = "Testmaterial";
        position.quantity = BigDecimal.ONE;
        position.unitPrice = new BigDecimal("25.00");
        position.positionOrder = 1;
        offer.positions.add(position);

        OfferStatusHistoryEntity history =
                new OfferStatusHistoryEntity();
        history.offer = offer;
        history.status = "DRAFT";
        history.changedAt = offer.createdAt;
        offer.statusHistory.add(history);

        offer.correctionSuggestions.add("Testhinweis");
        offer.customerChangeRequests.add("Teständerung");

        repository.persistAndFlush(offer);
        entityManager.clear();

        OfferEntity loaded = repository
                .findByBusinessKey(offer.businessKey)
                .orElseThrow();

        assertEquals("DRAFT", loaded.status);
        assertEquals("customer-1", loaded.customerId);
        assertEquals("Testmaterial", loaded.positions.get(0).name);
        assertEquals(1, loaded.statusHistory.size());
        assertEquals("Testhinweis",
                loaded.correctionSuggestions.get(0));
        assertEquals("Teständerung",
                loaded.customerChangeRequests.get(0));
    }
}
