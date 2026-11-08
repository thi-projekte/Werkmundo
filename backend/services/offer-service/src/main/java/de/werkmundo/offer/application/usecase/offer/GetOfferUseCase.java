package de.werkmundo.offer.application.usecase.offer;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.NoSuchElementException;
import java.util.Objects;

@ApplicationScoped
public class GetOfferUseCase {
    private final OfferRepository offerRepository;

    @Inject
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
