package de.werkmundo.offer.infrastructure.persistence;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import de.werkmundo.offer.infrastructure.OfferJpaRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(OfferPostgresTestProfile.class)
@EnabledIfEnvironmentVariable(named = "OFFER_DB_ENABLED", matches = "true")
class OfferEntityRepositoryTest {
    @Inject OfferRepository repository;
    @Inject EntityManager entityManager;

    @Test
    @TestTransaction
    void structuredRepositoryWritesTheExistingApplicationsTable() {
        assertInstanceOf(OfferJpaRepository.class, repository);
        String key = "shared-" + UUID.randomUUID();
        Offer offer = new Offer(key);
        offer.setCraftsmanId("shared-test-owner");
        repository.create(offer);
        entityManager.clear();

        var row = entityManager.createQuery("from Offer where businessKey = :key",
                de.winfprojekt.craftvoice.offerservice.offer.Offer.class)
                .setParameter("key", key).getSingleResult();
        assertEquals("shared-test-owner", row.handwerkerId);
        assertEquals("ENTWURF", row.status);
        assertEquals(OfferStatus.DRAFT, repository.findByBusinessKey(key).orElseThrow().getStatus());
    }

    @Test
    @TestTransaction
    void existingProcessOfferIsVisibleWithoutLosingItsPreciseStatusOrHistoryNote() {
        String key = "process-" + UUID.randomUUID();
        var row = new de.winfprojekt.craftvoice.offerservice.offer.Offer();
        row.businessKey = key;
        row.handwerkerId = "process-test-owner";
        row.status = "IN_BEARBEITUNG";
        var history = new de.winfprojekt.craftvoice.offerservice.offer.OfferStatusHistory();
        history.offer = row;
        history.status = "IN_BEARBEITUNG";
        history.notiz = "Original process note";
        row.statusHistory.add(history);
        entityManager.persist(row);
        entityManager.flush();
        entityManager.clear();

        Offer loaded = repository.findByBusinessKey(key).orElseThrow();
        assertEquals(OfferStatus.PROCESSING, loaded.getStatus());
        repository.save(loaded);
        entityManager.clear();
        var reloaded = entityManager.find(de.winfprojekt.craftvoice.offerservice.offer.Offer.class, row.id);
        assertEquals("IN_BEARBEITUNG", reloaded.status);
        assertEquals("Original process note", reloaded.statusHistory.getFirst().notiz);
    }
}