package de.werkmundo.invoice.document.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record DocumentSnapshot(
        LocalDate documentDate,
        String invoiceNumber,
        Party issuer,
        Party customer,
        List<LineItem> items,
        BigDecimal netAmount,
        List<TaxLine> taxLines,
        BigDecimal totalAmount,
        String currency,
        String notes,
        String referenceNumber) {

    public DocumentSnapshot {
        Objects.requireNonNull(documentDate, "documentDate");
        Objects.requireNonNull(customer, "customer");
        items = List.copyOf(items);
        Objects.requireNonNull(netAmount, "netAmount");
        taxLines = List.copyOf(taxLines);
        Objects.requireNonNull(totalAmount, "totalAmount");
        Objects.requireNonNull(currency, "currency");
    }

    public record Party(String name, String address, String email) {
    }

    public record LineItem(
            String description,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }

    public record TaxLine(String label, BigDecimal rate, BigDecimal amount) {
    }
}
