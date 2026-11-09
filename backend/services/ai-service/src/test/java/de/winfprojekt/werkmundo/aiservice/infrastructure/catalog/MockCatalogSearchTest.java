package de.winfprojekt.werkmundo.aiservice.infrastructure.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockCatalogSearchTest {

    private final MockCatalogSearch service = new MockCatalogSearch(new ObjectMapper());

    @Test
    void laedtGebuendeltenKatalog() {
        assertFalse(MockCatalogSearch.ladeKatalog(new ObjectMapper()).isEmpty());
    }

    @Test
    void findetPassendeKandidaten_undRanktRelevanteOben() {
        List<ProduktKandidat> result = service.search("Feinsteinzeug Eiche Bodenfliese", 5, null);
        assertFalse(result.isEmpty());
        assertEquals("FLI-1001", result.get(0).articleNumber());
        assertEquals("1001", result.get(0).id());
    }

    @Test
    void findetElektroKandidaten() {
        assertTrue(service.search("Doppelsteckdose Unterputz", 10, null).stream()
                .anyMatch(c -> "ELE-3001".equals(c.articleNumber())));
    }

    @Test
    void respektiertLimit() {
        assertTrue(service.search("Fliese Boden weiss grau", 2, null).size() <= 2);
    }

    @Test
    void keinTreffer_liefertLeereListe() {
        assertTrue(service.search("Quantencomputer Raumschiff", 5, null).isEmpty());
    }

    @Test
    void leereQuery_liefertLeereListe() {
        assertTrue(service.search("   ", 5, null).isEmpty());
    }
}
