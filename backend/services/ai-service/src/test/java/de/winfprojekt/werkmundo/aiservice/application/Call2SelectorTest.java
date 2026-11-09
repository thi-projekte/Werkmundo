package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.application.port.CatalogSearchPort;
import de.winfprojekt.werkmundo.aiservice.application.port.LlmPort;
import de.winfprojekt.werkmundo.aiservice.domain.Position;
import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Auswahl-Logik von LLM-Call 2 pro Position (ohne Executor). */
class Call2SelectorTest {

    private CatalogSearchPort catalog;
    private LlmPort llm;
    private Call2Selector selector;

    private static final Position POSITION = new Position("Feinsteinzeug 60x60", "Bodenfliese", 15.0, "m2");
    private static final List<ProduktKandidat> KANDIDATEN = List.of(
            new ProduktKandidat("1001", "FLI-1001", "Feinsteinzeug Eiche", "desc", "m2", "Fliesen"),
            new ProduktKandidat("1002", "FLI-1002", "Feinsteinzeug Beton", "desc", "m2", "Fliesen"));

    @BeforeEach
    void setUp() {
        catalog = Mockito.mock(CatalogSearchPort.class);
        llm = Mockito.mock(LlmPort.class);
        selector = new Call2Selector(llm, catalog, new Call2PromptBuilder(new PromptTemplates()),
                new Call2AnswerParser(new ObjectMapper()), null);
        when(catalog.search(anyString(), anyInt(), any())).thenReturn(KANDIDATEN);
    }

    @Test
    void ohneLlm_nimmtTopKandidat() {
        when(llm.isConfigured()).thenReturn(false);
        assertEquals("1001", selector.selectFor(POSITION, "hw-1").katalogProduktId());
    }

    @Test
    void mitLlm_nimmtGewaehltenKandidat() {
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenReturn("{\"articleNumber\":\"FLI-1002\"}");
        assertEquals("1002", selector.selectFor(POSITION, "hw-1").katalogProduktId());
    }

    @Test
    void mitLlm_keinTreffer_laesstPositionOhneId() {
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenReturn("{\"articleNumber\":\"KEIN_TREFFER\"}");
        assertNull(selector.selectFor(POSITION, "hw-1").katalogProduktId());
    }

    @Test
    void mitLlm_erfundeneNummer_wirdVerworfen() {
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenReturn("{\"articleNumber\":\"FLI-9999\"}");
        assertNull(selector.selectFor(POSITION, "hw-1").katalogProduktId());
    }

    @Test
    void llmFehler_nimmtTopKandidat() {
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenThrow(new LlmException("timeout"));
        assertEquals("1001", selector.selectFor(POSITION, "hw-1").katalogProduktId());
    }

    @Test
    void keineKandidaten_lassenPositionUnveraendert() {
        when(catalog.search(anyString(), anyInt(), any())).thenReturn(List.of());
        Position result = selector.selectFor(new Position("Exotenartikel", "gibt es nicht", 1.0, "Stk"), "hw-1");
        assertNull(result.katalogProduktId());
        assertEquals("Exotenartikel", result.bezeichnung());
    }
}
