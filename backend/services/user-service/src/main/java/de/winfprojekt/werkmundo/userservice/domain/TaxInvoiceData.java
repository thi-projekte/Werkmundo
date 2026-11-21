package de.winfprojekt.werkmundo.userservice.domain;

import java.math.BigDecimal;

/** Eingabewerte für {@link TaxInvoiceSettings}; geprüft wird beim Übernehmen. */
public record TaxInvoiceData(
        VatTreatment vatTreatment,
        BigDecimal defaultVatRate,
        String taxNumber,
        String vatId,
        String invoicePrefix,
        String offerPrefix,
        int paymentTermDays,
        String invoiceEmail) {
}
