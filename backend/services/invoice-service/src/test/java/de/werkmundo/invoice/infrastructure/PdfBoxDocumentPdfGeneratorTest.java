package de.werkmundo.invoice.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.LineItem;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.Party;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.TaxLine;
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
                null,
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
                new Party("Werkmundo GmbH", "Werkstraße 1, 12345 Berlin", "firma@example.com"),
                new Party("Ada Lovelace", "Berlin", "ada@example.com"),
                List.of(),
                BigDecimal.ZERO,
                List.of(),
                BigDecimal.ZERO,
                "EUR",
                "ENTWURF: Keine Angebotspositionen vorhanden. Dieses Dokument ist nicht versandfertig.",
                "WM-2026-001");

        byte[] pdf = new PdfBoxDocumentPdfGenerator().generate(DocumentType.OFFER, snapshot);

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Angebot # WM-2026-001"));
            assertTrue(text.contains("Werkmundo GmbH"));
            assertTrue(text.contains("Werkstraße 1, 12345 Berlin"));
            assertTrue(text.contains("firma@example.com"));
            assertTrue(text.contains("ABSENDER"));
            assertTrue(text.contains("EMPFÄNGER"));
            assertTrue(text.contains("23.11.2026"));
            assertFalse(text.contains("GÜLTIG BIS"));
            assertTrue(text.contains("BESCHREIBUNG"));
            assertTrue(text.contains("Gesamtpreis"));
            assertTrue(text.contains("0,00 €"));
            assertTrue(text.contains("nicht versandfertig"));
        }
    }

    @Test
    void generatesOfferPdfWithPositionAndTaxSummary() throws Exception {
        var snapshot = new DocumentSnapshot(
                LocalDate.of(2026, 11, 23),
                null,
                null,
                new Party("Ada Lovelace", "Berlin", "ada@example.com"),
                List.of(new LineItem(
                        "Montagearbeiten",
                        new BigDecimal("2"),
                        "Std.",
                        new BigDecimal("35.00"),
                        new BigDecimal("70.00"))),
                new BigDecimal("70.00"),
                List.of(new TaxLine("MwSt.", new BigDecimal("19"), new BigDecimal("13.30"))),
                new BigDecimal("83.30"),
                "EUR",
                null,
                "WM-2026-002");

        byte[] pdf = new PdfBoxDocumentPdfGenerator().generate(DocumentType.OFFER, snapshot);

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Angebot # WM-2026-002"));
            assertTrue(text.contains("Montagearbeiten"));
            assertTrue(text.contains("70,00 €"));
            assertTrue(text.contains("13,30 €"));
            assertTrue(text.contains("83,30 €"));
        }
    }
}
