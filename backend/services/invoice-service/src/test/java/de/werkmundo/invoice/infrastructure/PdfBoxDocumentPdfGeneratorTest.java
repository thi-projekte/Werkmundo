package de.werkmundo.invoice.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.LineItem;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.Party;
import de.werkmundo.invoice.document.domain.DocumentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxDocumentPdfGeneratorTest {
    @Test
    void generatesInvoicePdfFromDocumentSnapshot() throws Exception {
        var snapshot = new DocumentSnapshot(
                LocalDate.of(2026, 11, 23),
                "R-123",
                null,
                new Party("Ada Lovelace", "Berlin", "ada@example.com"),
                List.of(new LineItem(
                        "Material",
                        new BigDecimal("2"),
                        "Stk.",
                        new BigDecimal("15.00"),
                        new BigDecimal("30.00"))),
                new BigDecimal("30.00"),
                List.of(),
                new BigDecimal("30.00"),
                "EUR",
                null);

        byte[] pdf = new PdfBoxDocumentPdfGenerator().generate(DocumentType.INVOICE, snapshot);

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Invoice"));
            assertTrue(text.contains("Invoice number (placeholder): R-123"));
            assertTrue(text.contains("Ada Lovelace"));
            assertTrue(text.contains("30.00 EUR"));
        }
    }

    @Test
    void generatesOfferPdfWithoutPositions() throws Exception {
        var snapshot = new DocumentSnapshot(
                LocalDate.of(2026, 11, 23),
                null,
                null,
                new Party("Ada Lovelace", "Berlin", "ada@example.com"),
                List.of(),
                BigDecimal.ZERO,
                List.of(),
                BigDecimal.ZERO,
                "EUR",
                "ENTWURF: Keine Angebotspositionen vorhanden. Dieses Dokument ist nicht versandfertig.");

        byte[] pdf = new PdfBoxDocumentPdfGenerator().generate(DocumentType.OFFER, snapshot);

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Offer"));
            assertTrue(text.contains("Total: 0.00 EUR"));
            assertTrue(text.contains("nicht versandfertig"));
        }
    }
}
