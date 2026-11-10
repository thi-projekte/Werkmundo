package de.werkmundo.offer.application.usecase.offer;

import de.werkmundo.offer.application.command.CreateOfferPositionCommand;
import de.werkmundo.offer.application.command.UpdateOfferCommand;
import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.NoSuchElementException;

@ApplicationScoped
public class UpdateOfferUseCase {
    private final OfferRepository offerRepository;

    @Inject
    public UpdateOfferUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public Offer execute(UpdateOfferCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Offer updatedOffer = new Offer(command.businessKey());
        updatedOffer.setCustomerId(command.customerId());
        updatedOffer.setCraftsmanId(command.craftsmanId());
        updatedOffer.setEstimatedWorkDurationHours(command.estimatedWorkDurationHours());
        command.correctionSuggestions().forEach(updatedOffer::addCorrectionSuggestion);
        command.positions().forEach(positionCommand -> updatedOffer.addPosition(new OfferPosition(
                positionCommand.type(),
                positionCommand.name(),
                positionCommand.description(),
                positionCommand.manufacturer(),
                positionCommand.quantity(),
                positionCommand.unit(),
                positionCommand.catalogProductId(),
                positionCommand.unitPrice(),
                positionCommand.positionOrder())));

        Offer existingOffer = offerRepository.findByBusinessKey(updatedOffer.getBusinessKey())
                .orElseThrow(() -> new NoSuchElementException(
                        "Offer not found: " + updatedOffer.getBusinessKey()));

        existingOffer.updateDetailsFrom(updatedOffer);
        return offerRepository.save(existingOffer);
    }
}