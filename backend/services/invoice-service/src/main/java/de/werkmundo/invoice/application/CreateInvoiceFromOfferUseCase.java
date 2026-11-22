package de.werkmundo.invoice.application;

import de.werkmundo.invoice.domain.Invoice;
import de.werkmundo.invoice.domain.InvoiceCustomerSnapshot;
import de.werkmundo.invoice.domain.InvoicePositionType;
import de.werkmundo.invoice.domain.InvoiceRepository;
import de.werkmundo.invoice.infrastructure.RemoteServiceErrors;
import de.werkmundo.invoice.infrastructure.offer.InvoiceOfferLoader;
import de.werkmundo.invoice.infrastructure.offer.OfferPositionResponse;
import de.werkmundo.invoice.infrastructure.user.UserServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CreateInvoiceFromOfferUseCase {

    private final InvoiceOfferLoader offerLoader;
    private final UserServiceClient userClient;
    private final CreateInvoiceDraftUseCase createDraft;
    private final Instance<InvoiceRepository> repositories;

    @Inject
    public CreateInvoiceFromOfferUseCase(
            InvoiceOfferLoader offerLoader,
            @RestClient UserServiceClient userClient,
            CreateInvoiceDraftUseCase createDraft,
            Instance<InvoiceRepository> repositories) {

        this.offerLoader = offerLoader;
        this.userClient = userClient;
        this.createDraft = createDraft;
        this.repositories = repositories;
    }

    // Bestehende interne Aufrufe behandeln wir als neue Erstellungsaufträge.
    public Invoice create(
            String businessKey,
            String ownerId,
            String authorization) {

        return create(
                businessKey, ownerId, authorization, UUID.randomUUID());
    }

    public Invoice create(
            String businessKey,
            String ownerId,
            String authorization,
            UUID idempotencyKey) {

        validateRequest(businessKey, ownerId, authorization, idempotencyKey);

        var existing = repositories.get()
                .findByIdempotencyKey(ownerId, idempotencyKey);

        if (existing.isPresent()) {
            Invoice invoice = existing.get();

            if (!businessKey.equals(invoice.offerBusinessKey())) {
                throw new WebApplicationException(
                        "Der Idempotenzschlüssel wurde bereits "
                                + "für ein anderes Angebot verwendet",
                        409);
            }

            return invoice;
        }

        var offer = offerLoader.load(businessKey, ownerId, authorization);
        Long customerId = parseCustomerId(offer.customerId());

        var customer = RemoteServiceErrors.call(
                "User-Service",
                () -> userClient.getCustomer(customerId, authorization)
        );

        if (customer == null || !customerId.equals(customer.id())) {
            throw new WebApplicationException(
                    "Der User-Service hat keine passenden Kundendaten geliefert",
                    502);
        }

        var snapshot = new InvoiceCustomerSnapshot(
                customer.firstName(),
                customer.lastName(),
                customer.email(),
                customer.street(),
                customer.houseNumber(),
                customer.zipCode(),
                customer.city()
        );

        List<InvoicePositionInput> positions = offer.positions().stream()
                .map(this::requirePosition)
                .sorted(Comparator.comparing(
                        OfferPositionResponse::reihenfolge,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toInput)
                .toList();

        return createDraft.create(
                ownerId,
                offer.businessKey(),
                snapshot,
                positions,
                idempotencyKey
        );
    }

    private void validateRequest(
            String businessKey,
            String ownerId,
            String authorization,
            UUID idempotencyKey) {

        if (businessKey == null || businessKey.isBlank()) {
            throw new BadRequestException(
                    "Die Angebotsreferenz darf nicht fehlen");
        }

        if (idempotencyKey == null) {
            throw new BadRequestException(
                    "Der Idempotenzschlüssel darf nicht fehlen");
        }

        if (ownerId == null || ownerId.isBlank()) {
            throw new NotAuthorizedException("Bearer");
        }

        if (authorization == null
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) {
            throw new NotAuthorizedException("Bearer");
        }
    }

    private Long parseCustomerId(String value) {
        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;
        } catch (NumberFormatException exception) {
            throw new WebApplicationException(
                    "Das Angebot enthält keine gültige Kunden-ID",
                    422);
        }
    }

    private OfferPositionResponse requirePosition(
            OfferPositionResponse position) {

        if (position == null) {
            throw new WebApplicationException(
                    "Das Angebot enthält eine leere Position",
                    422);
        }

        return position;
    }

    private InvoicePositionInput toInput(OfferPositionResponse position) {
        InvoicePositionType type;

        try {
            if (position.type() == null) {
                throw new IllegalArgumentException();
            }

            type = InvoicePositionType.valueOf(position.type());
        } catch (IllegalArgumentException exception) {
            throw new WebApplicationException(
                    "Das Angebot enthält einen ungültigen Positionstyp",
                    422);
        }

        return new InvoicePositionInput(
                position.hersteller(),
                position.bezeichnung(),
                position.menge(),
                position.einheit(),
                position.katalogProduktId(),
                position.einzelPreis(),
                type
        );
    }
}