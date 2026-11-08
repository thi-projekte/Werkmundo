package de.werkmundo.offer.application;

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

class CancelOfferUseCaseTest {

    @Test
    void cancelsDraftOfferAndSavesIt() {
        Offer offer = new Offer("offer-123");
        AtomicInteger saveCalls = new AtomicInteger();
        CancelOfferUseCase useCase = new CancelOfferUseCase(repositoryFor(offer, saveCalls));

        Offer result = useCase.execute("offer-123");

        assertSame(offer, result);
        assertEquals(OfferStatus.CANCELLED, result.getStatus());
        assertEquals(2, result.getStatusHistory().size());
        assertEquals(1, saveCalls.get());
    }

    @Test
    void doesNotCancelOfferWithFinalStatus() {
        Offer offer = new Offer("offer-123");
        offer.markReadyToSend();
        offer.markSent();
        offer.acceptByCustomer();
        AtomicInteger saveCalls = new AtomicInteger();
        CancelOfferUseCase useCase = new CancelOfferUseCase(repositoryFor(offer, saveCalls));

        assertThrows(IllegalStateException.class, () -> useCase.execute("offer-123"));

        assertEquals(OfferStatus.ACCEPTED_BY_CUSTOMER, offer.getStatus());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        CancelOfferUseCase useCase = new CancelOfferUseCase(repositoryFor(null, new AtomicInteger()));

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
