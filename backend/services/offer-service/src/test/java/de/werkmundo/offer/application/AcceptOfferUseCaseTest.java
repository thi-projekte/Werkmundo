package de.werkmundo.offer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AcceptOfferUseCaseTest {

    @Test
    void acceptsSentOfferForCustomer() {
        Offer offer = new Offer("offer-123");
        offer.markReadyToSend();
        offer.markSent();
        AtomicInteger saveCalls = new AtomicInteger();
        AcceptOfferUseCase useCase = new AcceptOfferUseCase(repositoryFor(offer, saveCalls));

        Offer approvedOffer = useCase.execute("offer-123");

        assertSame(offer, approvedOffer);
        assertEquals(OfferStatus.ACCEPTED_BY_CUSTOMER, approvedOffer.getStatus());
        assertEquals(4, approvedOffer.getStatusHistory().size());
        assertEquals(1, saveCalls.get());
    }

    @Test
    void doesNotAcceptOfferWithDifferentStatus() {
        Offer offer = new Offer("offer-123");
        AtomicInteger saveCalls = new AtomicInteger();
        AcceptOfferUseCase useCase = new AcceptOfferUseCase(repositoryFor(offer, saveCalls));

        assertThrows(IllegalStateException.class, () -> useCase.execute("offer-123"));

        assertEquals(OfferStatus.DRAFT, offer.getStatus());
        assertEquals(0, saveCalls.get());
    }

    @Test
    void throwsWhenOfferDoesNotExist() {
        OfferRepository repository = new OfferRepository() {
            @Override
            public Offer save(Offer offer) {
                return offer;
            }

            @Override
            public Optional<Offer> findByBusinessKey(String businessKey) {
                return Optional.empty();
            }

            @Override
            public List<Offer> findAllByCraftsmanId(String craftsmanId) {
                return List.of();
            }
        };
        AcceptOfferUseCase useCase = new AcceptOfferUseCase(repository);

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
                return businessKey.equals(offer.getBusinessKey()) ? Optional.of(offer) : Optional.empty();
            }

            @Override
            public List<Offer> findAllByCraftsmanId(String craftsmanId) {
                return List.of();
            }
        };
    }
}
