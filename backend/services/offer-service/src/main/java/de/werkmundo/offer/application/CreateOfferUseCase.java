package de.werkmundo.offer.application;

import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferRepository;
import java.util.Objects;

public class CreateOfferUseCase {
    private final OfferRepository offerRepository;

    public CreateOfferUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public Offer execute(CreateOfferCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Offer offer = new Offer(command.businessKey());
        offer.setCustomerId(command.customerId());
        offer.setCraftsmanId(command.craftsmanId());
        offer.setAcceptanceToken(command.acceptanceToken());
        offer.setEstimatedWorkDurationHours(command.estimatedWorkDurationHours());

        command.correctionSuggestions().forEach(offer::addCorrectionSuggestion);
        command.positions().forEach(positionCommand -> {
            OfferPosition position = new OfferPosition(
                    positionCommand.type(),
                    positionCommand.name(),
                    positionCommand.description(),
                    positionCommand.manufacturer(),
                    positionCommand.quantity(),
                    positionCommand.unit(),
                    positionCommand.catalogProductId(),
                    positionCommand.unitPrice(),
                    positionCommand.positionOrder());
            position.setOffer(offer);
            offer.addPosition(position);
        });

        return offerRepository.save(offer);
    }
}