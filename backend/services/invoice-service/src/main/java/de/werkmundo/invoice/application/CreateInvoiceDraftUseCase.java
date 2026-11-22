package de.werkmundo.invoice.application;

import de.werkmundo.invoice.domain.Invoice;
import de.werkmundo.invoice.domain.InvoiceCustomerSnapshot;
import de.werkmundo.invoice.domain.InvoiceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class CreateInvoiceDraftUseCase {

    private final Instance<InvoiceRepository> repositories;

    @Inject
    public CreateInvoiceDraftUseCase(
            Instance<InvoiceRepository> repositories) {
        this.repositories = repositories;
    }

    public Invoice create(
            String ownerId,
            String offerBusinessKey,
            InvoiceCustomerSnapshot customer,
            List<InvoicePositionInput> inputs) {

        Invoice invoice = buildDraft(
                ownerId, offerBusinessKey, customer, inputs);

        repositories.get().insert(invoice);

        return invoice;
    }

    public Invoice create(
            String ownerId,
            String offerBusinessKey,
            InvoiceCustomerSnapshot customer,
            List<InvoicePositionInput> inputs,
            UUID idempotencyKey) {

        Objects.requireNonNull(
                idempotencyKey, "idempotencyKey darf nicht fehlen");

        Invoice invoice = buildDraft(
                ownerId, offerBusinessKey, customer, inputs);

        return repositories.get().insertOrGet(invoice, idempotencyKey);
    }

    private Invoice buildDraft(
            String ownerId,
            String offerBusinessKey,
            InvoiceCustomerSnapshot customer,
            List<InvoicePositionInput> inputs) {

        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Die Besitzerkennung darf nicht fehlen");
        }

        if (offerBusinessKey == null || offerBusinessKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Die Angebotsreferenz darf nicht fehlen");
        }

        if (customer == null) {
            throw new IllegalArgumentException(
                    "Der Kundendaten-Snapshot darf nicht fehlen");
        }

        var positions = InvoiceCalculator.calculatePositions(inputs);
        var netTotal = InvoiceCalculator.calculateNetTotal(positions);
        Instant now = Instant.now();

        return new Invoice(
                UUID.randomUUID(),
                ownerId,
                offerBusinessKey,
                null,
                customer,
                netTotal,
                now,
                now,
                positions
        );
    }
}