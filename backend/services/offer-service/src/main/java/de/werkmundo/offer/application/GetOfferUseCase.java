package de.werkmundo.offer.application;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import java.util.NoSuchElementException;
import java.util.Objects;

public class GetOfferUseCase {
    private final OfferRepository offerRepository;

    public GetOfferUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public Offer execute(String businessKey, String craftsmanId) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");
        Objects.requireNonNull(craftsmanId, "craftsmanId must not be null");

        Offer offer = offerRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NoSuchElementException("Offer not found"));

        if (!craftsmanId.equals(offer.getCraftsmanId())) {
            throw new NoSuchElementException("Offer not found");
        }

        return offer;
    }
}
