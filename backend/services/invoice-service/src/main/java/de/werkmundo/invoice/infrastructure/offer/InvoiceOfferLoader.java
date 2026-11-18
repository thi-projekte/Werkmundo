package de.werkmundo.invoice.infrastructure.offer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class InvoiceOfferLoader {

    private final OfferServiceClient offerClient;

    @Inject
    public InvoiceOfferLoader(@RestClient OfferServiceClient offerClient) {
        this.offerClient = offerClient;
    }

    public OfferResponse load(
            String businessKey,
            String ownerId,
            String authorization) {

        if (businessKey == null || businessKey.isBlank()) {
            throw new BadRequestException(
                    "Die Angebotsreferenz darf nicht fehlen");
        }

        if (ownerId == null || ownerId.isBlank()) {
            throw new NotAuthorizedException("Bearer");
        }

        if (authorization == null
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) {
            throw new NotAuthorizedException("Bearer");
        }

        OfferResponse offer = offerClient.getOffer(
                businessKey, authorization);

        if (offer == null || !businessKey.equals(offer.businessKey())) {
            throw new WebApplicationException(
                    "Der Offer-Service hat keine passende Angebotsantwort geliefert",
                    502);
        }

        if (!ownerId.equals(offer.handwerkerId())) {
            throw new ForbiddenException(
                    "Das Angebot gehört nicht zum angemeldeten Benutzer");
        }

        if (!"ANGENOMMEN".equals(offer.status())) {
            throw new WebApplicationException(
                    "Ein Rechnungsentwurf erfordert ein angenommenes Angebot",
                    409);
        }

        if (offer.customerId() == null || offer.customerId().isBlank()) {
            throw new WebApplicationException(
                    "Dem Angebot ist kein Kunde zugeordnet",
                    422);
        }

        if (offer.positions() == null || offer.positions().isEmpty()) {
            throw new WebApplicationException(
                    "Das Angebot enthält keine Positionen",
                    422);
        }

        return offer;
    }
}