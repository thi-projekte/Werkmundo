package de.werkmundo.offer.application;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import java.util.NoSuchElementException;
import java.util.Objects;

public class CancelOfferUseCase {
    private final OfferRepository offerRepository;

    public CancelOfferUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public Offer execute(String businessKey) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");

        Offer offer = offerRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + businessKey));

        offer.cancel();
        return offerRepository.save(offer);
    }
}
