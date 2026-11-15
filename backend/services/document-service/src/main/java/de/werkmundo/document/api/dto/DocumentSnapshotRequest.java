package de.werkmundo.document.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DocumentSnapshotRequest(
        String customerId,
        LocalDate documentDate,
        Party issuer,
        Party customer,
        List<LineItem> items,
        String currency,
        BigDecimal netAmount,
        List<TaxLine> taxLines,
        BigDecimal totalAmount,
        String notes) {

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
