package de.werkmundo.offer.application.usecase.customer;

import de.werkmundo.offer.application.security.OfferAccessTokenService;
import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Objects;

@ApplicationScoped
public class RequestOfferChangesUseCase {
    private final OfferRepository offerRepository;
    private final OfferAccessTokenService accessTokenService;
    private final Clock clock;

    @Inject
    public RequestOfferChangesUseCase(OfferRepository offerRepository) {
        this(offerRepository, new OfferAccessTokenService(), Clock.systemUTC());
    }

    public RequestOfferChangesUseCase(
            OfferRepository offerRepository,
            OfferAccessTokenService accessTokenService,
            Clock clock) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
        this.accessTokenService = Objects.requireNonNull(accessTokenService, "accessTokenService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Offer execute(String businessKey, String accessToken, String changeRequest) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(changeRequest, "changeRequest must not be null");

        Offer offer = offerRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + businessKey));
        accessTokenService.requireValid(offer, accessToken, clock.instant());

        offer.requestChanges(changeRequest);
        return offerRepository.save(offer);
    }
}
