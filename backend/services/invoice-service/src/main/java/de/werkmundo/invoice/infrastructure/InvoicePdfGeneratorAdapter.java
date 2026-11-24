package de.werkmundo.invoice.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import de.werkmundo.invoice.document.application.port.DocumentPdfGenerator;
import de.werkmundo.invoice.document.application.port.InvoicePdfGenerator;
import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.LineItem;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.Party;
import de.werkmundo.invoice.document.domain.DocumentType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class InvoicePdfGeneratorAdapter implements InvoicePdfGenerator {
    private final DocumentPdfGenerator documentPdfGenerator;

    @Inject
    public InvoicePdfGeneratorAdapter(DocumentPdfGenerator documentPdfGenerator) {
        this.documentPdfGenerator = documentPdfGenerator;
    }

    @Override
    public byte[] generate(JsonNode invoiceDraft) {
        return documentPdfGenerator.generate(DocumentType.INVOICE, toSnapshot(invoiceDraft));
    }

    private static DocumentSnapshot toSnapshot(JsonNode invoiceDraft) {
        if (invoiceDraft == null || !invoiceDraft.isObject()) {
            throw new BadRequestException("Invoice draft is required");
        }

        JsonNode customer = invoiceDraft.get("kundendaten");
        if (customer == null || !customer.isObject()) {
            throw new BadRequestException("Invoice customer data is required");
        }

        JsonNode positions = invoiceDraft.get("positions");
        if (positions == null || !positions.isArray() || positions.isEmpty()) {
            throw new BadRequestException("Invoice positions are required");
        }

        BigDecimal totalAmount = requiredAmount(invoiceDraft, "gesamtPreis");
        List<LineItem> items = new ArrayList<>();
        for (JsonNode position : positions) {
            if (!position.isObject()) {
                throw new BadRequestException("Invoice position must be an object");
            }
            items.add(new LineItem(
                    requiredText(position, "bezeichnung"),
                    requiredAmount(position, "menge"),
                    optionalText(position, "einheit"),
                    requiredAmount(position, "einzelPreis"),
                    requiredAmount(position, "positionsPreis")));
        }

        String createdAt = requiredText(invoiceDraft, "createdAt");
        if (createdAt.length() < 10) {
            throw new BadRequestException("Invoice creation date is invalid");
        }
        LocalDate documentDate;
        try {
            documentDate = LocalDate.parse(createdAt.substring(0, 10));
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Invoice creation date is invalid", exception);
        }

        return new DocumentSnapshot(
                documentDate,
                requiredText(invoiceDraft, "rechnungsnummer"),
                optionalParty(invoiceDraft.get("issuer")),
                new Party(
                        join(customer, "vorname", "nachname"),
                        join(customer, "strasse", "hausnummer", "plz", "ort"),
                        optionalText(customer, "email")),
                items,
                totalAmount,
                List.of(),
                totalAmount,
                "EUR",
                optionalText(invoiceDraft, "notes"),
                null);
    }

    private static Party optionalParty(JsonNode party) {
        if (party == null || party.isNull()) {
            return null;
        }
        if (!party.isObject()) {
            throw new BadRequestException("Invoice issuer must be an object");
        }
        return new Party(
                optionalText(party, "name"),
                optionalText(party, "address"),
                optionalText(party, "email"));
    }

    private static String join(JsonNode node, String... fields) {
        List<String> values = new ArrayList<>();
        for (String field : fields) {
            String value = optionalText(node, field);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return String.join(" ", values);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Invoice draft requires " + field);
        }
        return value;
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new BadRequestException("Invoice field " + field + " must be text");
        }
        return value.textValue();
    }

    private static BigDecimal requiredAmount(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) {
            throw new BadRequestException("Invoice field " + field + " must be a number");
        }
        return value.decimalValue();
    }
}
