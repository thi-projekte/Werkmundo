package de.werkmundo.offer.application;

import de.werkmundo.offer.application.security.GeneratedOfferAccessToken;
import de.werkmundo.offer.application.security.OfferAccessTokenService;
import de.werkmundo.offer.application.usecase.offer.MarkOfferSentUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MarkOfferSentUseCaseTest {

    @Test
    void marksReadyOfferAsSent() {
        Offer offer = new Offer("offer-123");
        offer.markReadyToSend();
        OfferAccessTokenService tokenService = new OfferAccessTokenService();
        GeneratedOfferAccessToken generatedToken = tokenService.generate();
        Instant expiresAt = Instant.parse("2026-12-08T12:00:00Z");
        offer.setCustomerAccessToken(generatedToken.tokenHash(), expiresAt);
        String storedTokenHash = offer.customerAccessTokenHash();
        AtomicInteger saveCalls = new AtomicInteger();
        MarkOfferSentUseCase useCase = new MarkOfferSentUseCase(repositoryFor(offer, saveCalls));

        Offer result = useCase.execute("offer-123");

        assertSame(offer, result);
        assertEquals(OfferStatus.SENT, result.getStatus());
        assertEquals(3, result.getStatusHistory().size());
        assertEquals(storedTokenHash, result.customerAccessTokenHash());
        assertEquals(expiresAt, result.customerAccessTokenExpiresAt());
        tokenService.requireValid(result, generatedToken.token(),
                Instant.parse("2026-11-08T12:00:00Z"));
        assertEquals(1, saveCalls.get());
    }

    @Test
    void rejectsMarkingOfferSentBeforeItIsReady() {
        Offer offer = new Offer("offer-123");
        AtomicInteger saveCalls = new AtomicInteger();
        MarkOfferSentUseCase useCase = new MarkOfferSentUseCase(repositoryFor(offer, saveCalls));

        assertThrows(IllegalStateException.class, () -> useCase.execute("offer-123"));

        assertEquals(OfferStatus.DRAFT, offer.getStatus());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        MarkOfferSentUseCase useCase = new MarkOfferSentUseCase(repositoryFor(null, new AtomicInteger()));

        assertThrows(NoSuchElementException.class, () -> useCase.execute("missing-offer"));
    }

    private OfferRepository repositoryFor(Offer offer, AtomicInteger saveCalls) {
        return new OfferRepository() {
            @Override
            public Offer save(Offer offerToSave) {
                saveCalls.incrementAndGet();
                return offerToSave;
            }

            @Override
            public Optional<Offer> findByBusinessKey(String businessKey) {
                return offer != null && businessKey.equals(offer.getBusinessKey())
                        ? Optional.of(offer)
                        : Optional.empty();
            }

            @Override
            public List<Offer> findAllByCraftsmanId(String craftsmanId) {
                return List.of();
            }
        };
    }
}
