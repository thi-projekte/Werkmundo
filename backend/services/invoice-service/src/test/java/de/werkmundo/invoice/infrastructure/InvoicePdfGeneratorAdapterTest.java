package de.werkmundo.invoice.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentType;
import jakarta.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InvoicePdfGeneratorAdapterTest {
    private static final byte[] PDF = {1, 2, 3};

    @Test
    void mapsInvoiceDraftAndDelegatesPdfGeneration() throws Exception {
        AtomicReference<DocumentSnapshot> receivedSnapshot = new AtomicReference<>();
        var adapter = new InvoicePdfGeneratorAdapter((type, snapshot) -> {
            assertEquals(DocumentType.INVOICE, type);
            receivedSnapshot.set(snapshot);
            return PDF;
        });

        byte[] result = adapter.generate(new ObjectMapper().readTree("""
                {
                  "createdAt": "2026-11-23T12:30:00",
                  "rechnungsnummer": "R-123",
                  "gesamtPreis": 30.00,
                  "kundendaten": {
                    "vorname": "Ada",
                    "nachname": "Lovelace",
                    "strasse": "Hauptstrasse",
                    "hausnummer": "1",
                    "plz": "12345",
                    "ort": "Berlin",
                    "email": "ada@example.com"
                  },
                  "positions": [{
                    "bezeichnung": "Material",
                    "menge": 2,
                    "einheit": "Stk.",
                    "einzelPreis": 15.00,
                    "positionsPreis": 30.00
                  }]
                }
                """));

        assertSame(PDF, result);
        DocumentSnapshot snapshot = receivedSnapshot.get();
        assertEquals(LocalDate.of(2026, 11, 23), snapshot.documentDate());
        assertEquals("R-123", snapshot.invoiceNumber());
        assertEquals("Ada Lovelace", snapshot.customer().name());
        assertEquals("Hauptstrasse 1 12345 Berlin", snapshot.customer().address());
        assertEquals("ada@example.com", snapshot.customer().email());
        assertEquals(new BigDecimal("2"), snapshot.items().get(0).quantity());
        assertEquals(0, new BigDecimal("30.00").compareTo(snapshot.totalAmount()));
        assertEquals("EUR", snapshot.currency());
    }

    @Test
    void rejectsInvoiceDraftWithoutInvoiceNumber() throws Exception {
        var adapter = new InvoicePdfGeneratorAdapter((type, snapshot) -> PDF);

        assertThrows(BadRequestException.class,
                () -> adapter.generate(new ObjectMapper().readTree("""
                        {
                          "createdAt": "2026-11-23T12:30:00",
                          "gesamtPreis": 0,
                          "kundendaten": {},
                          "positions": [{
                            "bezeichnung": "Material",
                            "menge": 1,
                            "einzelPreis": 0,
                            "positionsPreis": 0
                          }]
                        }
                        """)));
    }
}
