package de.werkmundo.offer.application.usecase.offer;

import de.werkmundo.offer.application.security.GeneratedOfferAccessToken;
import de.werkmundo.offer.application.security.OfferAccessTokenService;
import de.werkmundo.offer.domain.Offer;
import de.werkmundo.offer.domain.OfferRepository;
import de.werkmundo.offer.domain.OfferStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@ApplicationScoped
public class ApproveOfferUseCase {
    public static final Duration DEFAULT_TOKEN_LIFETIME = Duration.ofDays(30);

    private final OfferRepository offerRepository;
    private final OfferAccessTokenService accessTokenService;
    private final Duration tokenLifetime;
    private final Clock clock;

    @Inject
    public ApproveOfferUseCase(OfferRepository offerRepository) {
        this(offerRepository, DEFAULT_TOKEN_LIFETIME);
    }

    public ApproveOfferUseCase(OfferRepository offerRepository, Duration tokenLifetime) {
        this(offerRepository, tokenLifetime, Clock.systemUTC(), new OfferAccessTokenService());
    }

    public ApproveOfferUseCase(
            OfferRepository offerRepository,
            Duration tokenLifetime,
            Clock clock,
            OfferAccessTokenService accessTokenService) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository must not be null");
        this.tokenLifetime = Objects.requireNonNull(tokenLifetime, "tokenLifetime must not be null");
        if (tokenLifetime.isZero() || tokenLifetime.isNegative()) {
            throw new IllegalArgumentException("tokenLifetime must be positive");
        }
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.accessTokenService = Objects.requireNonNull(accessTokenService, "accessTokenService must not be null");
    }

    public ApproveOfferResult execute(String businessKey) {
        Objects.requireNonNull(businessKey, "businessKey must not be null");

        Offer offer = offerRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NoSuchElementException("Offer not found: " + businessKey));

        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new IllegalStateException("Only draft offers can be approved for sending");
        }

        GeneratedOfferAccessToken generatedToken = accessTokenService.generate();
        Instant expiresAt = clock.instant().plus(tokenLifetime);
        offer.markReadyToSend();
        offer.setCustomerAccessToken(generatedToken.tokenHash(), expiresAt);
        Offer savedOffer = offerRepository.save(offer);
        return new ApproveOfferResult(savedOffer, generatedToken.token(), expiresAt);
    }
}
