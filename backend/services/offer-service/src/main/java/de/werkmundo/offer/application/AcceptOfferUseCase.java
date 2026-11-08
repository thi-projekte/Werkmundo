package de.werkmundo.offer.application;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import java.util.NoSuchElementException;
import java.util.Objects;

public class AcceptOfferUseCase {
    private final OfferRepository offerRepository;

    public AcceptOfferUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public Offer execute(String businessKey) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");

        Offer offer = offerRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + businessKey));

        if (offer.getStatus() != OfferStatus.SENT) {
            throw new IllegalStateException("Only sent offers can be accepted by the customer");
        }

        offer.acceptByCustomer();
        return offerRepository.save(offer);
    }
}