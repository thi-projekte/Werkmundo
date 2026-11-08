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
import org.junit.jupiter.api.Test;

class RequestOfferChangesUseCaseTest {

    @Test
    void recordsCustomerChangeRequestAndSavesOffer() {
        Offer offer = new Offer("offer-123");
        offer.markReadyToSend();
        offer.markSent();
        OfferRepository repository = repositoryFor(offer);
        RequestOfferChangesUseCase useCase = new RequestOfferChangesUseCase(repository);

        Offer result = useCase.execute("offer-123", "Please update the delivery date");

        assertSame(offer, result);
        assertEquals(OfferStatus.CHANGE_REQUESTED, result.getStatus());
        assertEquals(1, result.getCustomerChangeRequests().size());
        assertEquals("Please update the delivery date", result.getCustomerChangeRequests().get(0));
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
        RequestOfferChangesUseCase useCase = new RequestOfferChangesUseCase(repository);

        assertThrows(NoSuchElementException.class,
                () -> useCase.execute("missing-offer", "Please update the delivery date"));
    }

    private OfferRepository repositoryFor(Offer offer) {
        return new OfferRepository() {
            @Override
            public Offer save(Offer offerToSave) {
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
