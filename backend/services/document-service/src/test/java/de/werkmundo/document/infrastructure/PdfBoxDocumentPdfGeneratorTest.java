package de.werkmundo.document.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.werkmundo.document.api.dto.DocumentSnapshotRequest;
import de.werkmundo.document.domain.DocumentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxDocumentPdfGeneratorTest {
    private final PdfBoxDocumentPdfGenerator generator = new PdfBoxDocumentPdfGenerator();

    @Test
    void createsInvoicePdfWithProvidedTaxBreakdown() throws Exception {
        byte[] pdf = generator.generate(DocumentType.INVOICE, snapshot());

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Invoice"));
            assertTrue(text.contains("VAT (19%): 19.00 EUR"));
            assertTrue(text.contains("Total: 119.00 EUR"));
        }
    }

    @Test
    void createsOrderConfirmationPdfWithoutCalculatingTaxes() throws Exception {
        byte[] pdf = generator.generate(
                DocumentType.ORDER_CONFIRMATION,
                new DocumentSnapshotRequest(
                        null,
                        LocalDate.of(2026, 11, 15),
                        new DocumentSnapshotRequest.Party("Werkmundo", null, null),
                        new DocumentSnapshotRequest.Party("Customer", null, "customer@example.com"),
                        List.of(new DocumentSnapshotRequest.LineItem(
                                "Repair", BigDecimal.ONE, "hour", new BigDecimal("100"), new BigDecimal("100"))),
                        "EUR",
                        new BigDecimal("100"),
                        List.of(),
                        new BigDecimal("100"),
                        null));

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Order confirmation"));
            assertTrue(text.contains("Net amount: 100.00 EUR"));
            org.junit.jupiter.api.Assertions.assertFalse(text.contains("VAT"));
        }
    }

    private static DocumentSnapshotRequest snapshot() {
        return new DocumentSnapshotRequest(
                "customer-1",
                LocalDate.of(2026, 11, 15),
                new DocumentSnapshotRequest.Party("Werkmundo", "Werkstrasse 1", "office@example.com"),
                new DocumentSnapshotRequest.Party("Customer", "Customer Street 2", "customer@example.com"),
                List.of(new DocumentSnapshotRequest.LineItem(
                        "Repair", BigDecimal.ONE, "hour", new BigDecimal("100"), new BigDecimal("100"))),
                "EUR",
                new BigDecimal("100"),
                List.of(new DocumentSnapshotRequest.TaxLine(
                        "VAT", new BigDecimal("19"), new BigDecimal("19"))),
                new BigDecimal("119"),
                null);
    }
}
