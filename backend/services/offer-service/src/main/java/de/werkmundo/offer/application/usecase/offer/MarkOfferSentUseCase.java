package de.werkmundo.offer.application.usecase.offer;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.NoSuchElementException;
import java.util.Objects;

@ApplicationScoped
public class MarkOfferSentUseCase {
    private final OfferRepository offerRepository;

    @Inject
    public MarkOfferSentUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public Offer execute(String businessKey) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");

        Offer offer = offerRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + businessKey));

        if (offer.getStatus() != OfferStatus.READY_TO_SEND) {
            throw new IllegalStateException("Only offers ready to send can be marked as sent");
        }

        offer.markSent();
        return offerRepository.save(offer);
    }
}
