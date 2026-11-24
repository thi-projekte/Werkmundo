package de.werkmundo.offer.infrastructure.persistence;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(OfferPostgresTestProfile.class)
@EnabledIfEnvironmentVariable(named = "OFFER_DB_ENABLED", matches = "true")
class OfferNumberConcurrencyTest {
    @Inject OfferRepository repository;

    @Test
    void concurrentSavesKeepOneOfferNumber() throws Exception {
        String key = "issue18-race-" + UUID.randomUUID();
        Offer draft = new Offer(key);
        draft.setCraftsmanId("issue18-race-owner");
        repository.create(draft);

        Offer first = repository.findByBusinessKey(key).orElseThrow();
        Offer second = repository.findByBusinessKey(key).orElseThrow();
        first.markReadyToSend();
        second.markReadyToSend();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(() -> saveSafely(first, start));
            Future<String> b = pool.submit(() -> saveSafely(second, start));
            start.countDown();
            String numberA = a.get(30, TimeUnit.SECONDS);
            String numberB = b.get(30, TimeUnit.SECONDS);
            assertTrue((numberA == null) != (numberB == null));
            String stored = repository.findByBusinessKey(key).orElseThrow().getOfferNumber();
            assertNotNull(stored);
            assertEquals(numberA != null ? numberA : numberB, stored);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private String saveSafely(Offer offer, CountDownLatch start) throws Exception {
        start.await();
        try {
            return repository.save(offer).getOfferNumber();
        } catch (IllegalStateException expected) {
            return null;
        }
    }
}