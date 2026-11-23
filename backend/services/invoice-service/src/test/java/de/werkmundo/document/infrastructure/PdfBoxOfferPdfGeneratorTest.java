package de.werkmundo.document.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxOfferPdfGeneratorTest {
    private final PdfBoxOfferPdfGenerator generator = new PdfBoxOfferPdfGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void embedsDemoWatermarkOnEveryPageAndUsesInputPositions() throws IOException {
        StringBuilder json = new StringBuilder("""
                {"businessKey":"offer-demo","title":"Badrenovierung","customer":{"name":"Musterkunde - Demo-Vorschau"},
                 "positions":[
                """);
        for (int index = 0; index < 40; index++) {
            if (index > 0) json.append(',');
            json.append("""
                    {"bezeichnung":"Fliesen verlegen %d","menge":2,"einheit":"m2"}
                    """.formatted(index));
        }
        json.append("]}");

        byte[] pdf = generator.generate(objectMapper.readTree(json.toString()), true);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() > 1);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                assertTrue(stripper.getText(document).contains("DEMO"));
            }
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Fliesen verlegen 0"));
            assertTrue(text.contains("Musterkunde - Demo-Vorschau"));
            assertTrue(text.contains("nicht zur geschaeftlichen Verwendung"));
            assertTrue(text.contains("Preis offen"));
        }
    }

    @Test
    void regularOfferUsesTheSameLayoutWithoutDemoWatermark() throws IOException {
        var offer = objectMapper.readTree("""
                {
                  "businessKey":"offer-regular",
                  "title":"Badrenovierung",
                  "seller":{"companyName":"Musterbetrieb"},
                  "customer":{"name":"Max Mustermann"},
                  "positions":[{"bezeichnung":"Badewanne austauschen","menge":1,"einheit":"Stk.","einzelPreis":125.5}]
                }
                """);

        byte[] pdf = generator.generate(offer, false);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Musterbetrieb"));
            assertTrue(text.contains("Max Mustermann"));
            assertTrue(text.contains("Badewanne austauschen"));
            assertFalse(text.contains("DEMO"));
        }
    }
}
