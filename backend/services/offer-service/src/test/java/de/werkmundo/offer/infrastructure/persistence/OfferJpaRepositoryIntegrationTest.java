package de.werkmundo.offer.infrastructure.persistence;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferPositionType;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Run with OFFER_PERSISTENCE=postgres and OFFER_DB_ENABLED=true. */
@QuarkusTest
@io.quarkus.test.junit.TestProfile(OfferPostgresTestProfile.class)
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "OFFER_DB_ENABLED", matches = "true")
class OfferJpaRepositoryIntegrationTest {
    @Inject
    OfferRepository repository;

    @Test
    @TestTransaction
    void createDoesNotOverwriteAnotherOwnersOffer() {
        String key = "duplicate-" + UUID.randomUUID();
        Offer original = new Offer(key);
        original.setCraftsmanId("original-owner");
        repository.create(original);
        Offer duplicate = new Offer(key);
        duplicate.setCraftsmanId("other-owner");

        assertThrows(de.werkmundo.offer.domain.OfferAlreadyExistsException.class,
                () -> repository.create(duplicate));
        assertEquals("original-owner", repository.findByBusinessKey(key).orElseThrow().getCraftsmanId());
    }

    @Test
    @TestTransaction
    void persistsAndRehydratesDomainOffer() {
        String key = "issue32-" + UUID.randomUUID();
        Offer offer = new Offer(key);
        offer.setCustomerId("customer-test");
        offer.setCraftsmanId("craftsman-test");
        offer.addCorrectionSuggestion("Check dimensions");
        offer.addPosition(new OfferPosition(
                OfferPositionType.MATERIAL, "Cable", "2m cable", "Test", BigDecimal.TWO,
                "m", "cat-1", new BigDecimal("8.50"), 0));
        repository.save(offer);

        Offer loaded = repository.findByBusinessKey(key).orElseThrow();
        assertEquals("customer-test", loaded.getCustomerId());
        assertEquals("craftsman-test", loaded.getCraftsmanId());
        assertEquals(OfferStatus.DRAFT, loaded.getStatus());
        assertEquals(1, loaded.getPositions().size());
        assertEquals(new BigDecimal("17.00"), loaded.getTotalPrice());
        assertEquals(1, loaded.getStatusHistory().size());
        assertEquals(1, loaded.getCorrectionSuggestions().size());
        assertTrue(repository.findAllByCraftsmanId("craftsman-test").stream()
                .anyMatch(x -> x.getBusinessKey().equals(key)));

        LocalDateTime createdAt = loaded.getCreatedAt();
        loaded.markReadyToSend();
        repository.save(loaded);
        Offer reloaded = repository.findByBusinessKey(key).orElseThrow();
        assertEquals(OfferStatus.READY_TO_SEND, reloaded.getStatus());
        assertNotNull(reloaded.getOfferNumber());
        assertTrue(reloaded.getOfferNumber().matches("^ANG-[0-9]{6}-[0-9]{4}$"));
        assertEquals(createdAt, reloaded.getCreatedAt());
        assertEquals(2, reloaded.getStatusHistory().size());
    }

    @Test
    @TestTransaction
    void assignsDistinctNumbersToOffers() {
        Offer first = new Offer("issue18-first-" + UUID.randomUUID());
        first.setCraftsmanId("issue18-test-owner");
        first.markReadyToSend();
        Offer savedFirst = repository.save(first);

        Offer second = new Offer("issue18-second-" + UUID.randomUUID());
        second.setCraftsmanId("issue18-test-owner");
        second.markReadyToSend();
        Offer savedSecond = repository.save(second);

        assertNotNull(savedFirst.getOfferNumber());
        assertNotNull(savedSecond.getOfferNumber());
        assertNotEquals(savedFirst.getOfferNumber(), savedSecond.getOfferNumber());
    }

    @Test
    void offerNumberPersistsAcrossTransactions() {
        String key = "issue18-committed-" + UUID.randomUUID();
        Offer offer = new Offer(key);
        offer.setCraftsmanId("issue18-test-owner");
        offer.markReadyToSend();
        Offer saved = repository.save(offer);
        assertNotNull(saved.getOfferNumber());

        Offer reloaded = repository.findByBusinessKey(key).orElseThrow();
        assertEquals(saved.getOfferNumber(), reloaded.getOfferNumber());
    }

    @Test
    @TestTransaction
    void cannotDeleteNumberedOffer() {
        String key = "issue18-protected-" + UUID.randomUUID();
        Offer offer = new Offer(key);
        offer.setCraftsmanId("issue18-test-owner");
        offer.markReadyToSend();
        Offer saved = repository.save(offer);
        assertNotNull(saved.getOfferNumber());

        assertThrows(IllegalStateException.class, () -> repository.deleteByBusinessKey(key));
        assertEquals(saved.getOfferNumber(),
                repository.findByBusinessKey(key).orElseThrow().getOfferNumber());
    }

    @Test
    @TestTransaction
    void deletesOfferAndItsRelatedData() {
        String key = "issue32-delete-" + UUID.randomUUID();
        Offer offer = new Offer(key);
        offer.setCraftsmanId("delete-test-owner");
        offer.addCorrectionSuggestion("Remove with offer");
        offer.addPosition(new OfferPosition(
                OfferPositionType.MATERIAL, "Cable", "2m cable", "Test", BigDecimal.ONE,
                "m", "cat-1", new BigDecimal("8.50"), 0));
        repository.save(offer);

        assertTrue(repository.deleteByBusinessKey(key));
        assertTrue(repository.findByBusinessKey(key).isEmpty());
        assertFalse(repository.deleteByBusinessKey(key));
    }
}
