package de.werkmundo.invoice.api;

import de.werkmundo.invoice.application.CreateInvoiceFromOfferUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.math.BigDecimal;
import java.util.UUID;

@Path("/invoices")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("OWNER")
public class InvoiceResource {

    @Inject
    CreateInvoiceFromOfferUseCase createInvoice;

    @Inject
    JsonWebToken jwt;

    @Inject
    de.werkmundo.invoice.infrastructure.InvoiceNumberIssuer numberIssuer;

    @POST
    @Path("/drafts")
    public InvoiceDraftResponse createDraft(
            CreateInvoiceDraftRequest request,
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {

        if (request == null
                || request.businessKey() == null
                || request.businessKey().isBlank()) {
            throw new BadRequestException(
                    "Die Angebotsreferenz darf nicht fehlen");
        }

        UUID key = parseIdempotencyKey(idempotencyKey);

        var invoice = createInvoice.create(
                request.businessKey(),
                jwt.getSubject(),
                authorization,
                key
        );

        return new InvoiceDraftResponse(
                invoice.id(),
                invoice.offerBusinessKey(),
                invoice.rechnungsnummer(),
                invoice.gesamtPreis(),
                "EUR"
        );
    }

    @POST
    @Path("/{invoiceId}/issue")
    public InvoiceIssueResponse issueInvoice(
            @jakarta.ws.rs.PathParam("invoiceId") UUID invoiceId) {
        String number = numberIssuer.issue(invoiceId, jwt.getSubject())
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Invoice not found"));
        return new InvoiceIssueResponse(invoiceId, number);
    }

    public record InvoiceIssueResponse(UUID id, String rechnungsnummer) {
    }

    private UUID parseIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(
                    "Der Header Idempotency-Key ist erforderlich");
        }

        try {
            UUID key = UUID.fromString(value);

            if (!key.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException();
            }

            return key;
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "Idempotency-Key muss eine UUID im Standardformat sein");
        }
    }

    public record InvoiceDraftResponse(
            UUID id,
            String offerBusinessKey,
            String rechnungsnummer,
            BigDecimal nettoGesamt,
            String waehrung
    ) {
    }
}