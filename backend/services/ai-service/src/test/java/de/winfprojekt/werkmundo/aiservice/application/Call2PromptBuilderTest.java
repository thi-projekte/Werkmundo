package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.domain.Position;
import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Call2PromptBuilderTest {

    private final Call2PromptBuilder builder = new Call2PromptBuilder(new PromptTemplates());
    private final Call2AnswerParser parser = new Call2AnswerParser(new ObjectMapper());

    private static ProduktKandidat cand(String art, String name) {
        return new ProduktKandidat("1", art, name, "Beschreibung " + name, "Stk", "Kategorie");
    }

    @Test
    void systemPrompt_erlaubtKeinTreffer_undIgnoriertPreise() {
        String s = builder.systemPrompt();
        assertTrue(s.contains("KEIN_TREFFER"));
        assertTrue(s.toLowerCase().contains("preise"));
    }

    @Test
    void userContent_listetKandidaten_ohnePreis_neutralSortiert() {
        String u = builder.userContent(new Position("Doppelsteckdose", "UP 2-fach", 3.0, "Stk"),
                List.of(cand("ELE-3002", "Wechselschalter"), cand("ELE-3001", "Doppelsteckdose UP")));

        assertTrue(u.contains("Doppelsteckdose"));
        assertTrue(u.indexOf("ELE-3001") < u.indexOf("ELE-3002"), "neutral nach articleNumber sortiert");
        assertFalse(u.toLowerCase().contains("preis"));
        assertFalse(u.contains("€"));
    }

    @Test
    void parsePick_ausSauberemJson() {
        assertEquals("FLI-1001", parser.parsePick("{\"articleNumber\":\"FLI-1001\",\"begruendung\":\"passt\"}"));
    }

    @Test
    void parsePick_mitMarkdownFences() {
        assertEquals("ELE-3001", parser.parsePick("```json\n{\"articleNumber\":\"ELE-3001\"}\n```"));
    }

    @Test
    void parsePick_echteKatalognummer() {
        assertEquals("INS-KAI-GD2-25", parser.parsePick("{\"articleNumber\":\"ins-kai-gd2-25\"}"));
    }

    @Test
    void parsePick_keinTreffer_ausJson() {
        assertEquals(Call2AnswerParser.KEIN_TREFFER, parser.parsePick("{\"articleNumber\":\"KEIN_TREFFER\"}"));
    }

    @Test
    void parsePick_keinTreffer_ausFreitext() {
        assertEquals(Call2AnswerParser.KEIN_TREFFER, parser.parsePick("Kein Treffer leider"));
    }

    @Test
    void parsePick_regexFallback_ausFreitext() {
        assertEquals("BOD-2001", parser.parsePick("Ich waehle BOD-2001 weil es am besten passt."));
    }

    @Test
    void parsePick_muell_wirdZuKeinTreffer() {
        assertEquals(Call2AnswerParser.KEIN_TREFFER, parser.parsePick("völliger Unsinn ohne Nummer"));
    }
}
