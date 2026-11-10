package de.werkmundo.offer.application;

import de.werkmundo.offer.application.security.OfferAccessTokenService;
import de.werkmundo.offer.application.usecase.customer.RejectOfferUseCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RejectOfferUseCaseTest {

    @Test
    void rejectsSentOfferAndSavesIt() {
        Offer offer = new Offer("offer-123");
        OfferAccessTokenService tokenService = new OfferAccessTokenService();
        String accessToken = OfferAccessTokenTestSupport.issueForSentOffer(offer, tokenService);
        AtomicInteger saveCalls = new AtomicInteger();
        RejectOfferUseCase useCase = new RejectOfferUseCase(
                repositoryFor(offer, saveCalls), tokenService, OfferAccessTokenTestSupport.CLOCK);

        Offer result = useCase.execute("offer-123", accessToken);

        assertSame(offer, result);
        assertEquals(OfferStatus.REJECTED_BY_CUSTOMER, result.getStatus());
        assertEquals(4, result.getStatusHistory().size());
        assertEquals(1, saveCalls.get());
    }

    @Test
    void doesNotRejectOfferThatWasNotSent() {
        Offer offer = new Offer("offer-123");
        AtomicInteger saveCalls = new AtomicInteger();
        RejectOfferUseCase useCase = new RejectOfferUseCase(repositoryFor(offer, saveCalls));

        assertThrows(SecurityException.class, () -> useCase.execute("offer-123", "invalid-token"));

        assertEquals(OfferStatus.DRAFT, offer.getStatus());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        RejectOfferUseCase useCase = new RejectOfferUseCase(repositoryFor(null, new AtomicInteger()));

        assertThrows(NoSuchElementException.class, () -> useCase.execute("missing-offer", "token"));
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
