package de.werkmundo.offer.application.usecase.offer;

import de.werkmundo.offer.domain.OfferRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.NoSuchElementException;
import java.util.Objects;

@ApplicationScoped
public class DeleteOfferUseCase {
    private final OfferRepository offerRepository;

    @Inject
    public DeleteOfferUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public void execute(String businessKey) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");

        if (!offerRepository.deleteByBusinessKey(businessKey)) {
            throw new NoSuchElementException("Offer not found: " + businessKey);
        }
    }
}
