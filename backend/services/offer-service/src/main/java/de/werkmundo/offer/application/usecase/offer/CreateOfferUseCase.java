package de.werkmundo.offer.application.usecase.offer;

import de.werkmundo.offer.application.command.CreateOfferCommand;
import de.werkmundo.offer.application.command.CreateOfferPositionCommand;
import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferPosition;
import de.werkmundo.offer.domain.OfferRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;

@ApplicationScoped
public class CreateOfferUseCase {
    private final OfferRepository offerRepository;

    @Inject
    public CreateOfferUseCase(OfferRepository offerRepository) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
    }

    public Offer execute(CreateOfferCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Offer offer = new Offer(command.businessKey());
        offer.setCustomerId(command.customerId());
        offer.setCraftsmanId(command.craftsmanId());
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

        return offerRepository.create(offer);
    }
}
