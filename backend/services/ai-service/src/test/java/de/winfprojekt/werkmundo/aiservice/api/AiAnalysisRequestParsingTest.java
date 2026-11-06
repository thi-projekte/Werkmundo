package de.winfprojekt.werkmundo.aiservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.api.dto.AiAnalysisRequest;
import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.ProcessType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** JSON-Mapping des Process-Engine-Payloads (contracts/ai/AiAnalysisRequest.json). */
class AiAnalysisRequestParsingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void erstangebot_payload_wird_gemappt() throws Exception {
        String json = """
                {
                  "businessKey": "BK-100",
                  "prompt": "Erstelle ein Erstangebot anhand von Vorlage und Sprachschnipsel.",
                  "vorlage": {
                    "leistungen": [ {"bezeichnung": "Fliesen verlegen", "beschreibung": "Stundensatz", "menge": 1, "einheit": "h"} ],
                    "material":   [ {"bezeichnung": "Feinsteinzeug 60x60", "beschreibung": "grossformatig", "menge": 15, "einheit": "m²", "preis": 39.9} ],
                    "notizen": ["grossformatig"]
                  },
                  "sprachschnipsel": "Im Bad neue Bodenfliesen verlegen."
                }
                """;
        AiAnalysisRequest req = mapper.readValue(json, AiAnalysisRequest.class);
        AnalyseAuftrag auftrag = req.toAuftrag("hw-1");

        assertEquals(ProcessType.ERSTANGEBOT, auftrag.typ());
        assertEquals("BK-100", auftrag.businessKey());
        assertEquals("Fliesen verlegen", auftrag.vorlage().leistungen().get(0).bezeichnung());
        assertEquals(15.0, auftrag.vorlage().material().get(0).menge());
        assertEquals("Im Bad neue Bodenfliesen verlegen.", auftrag.sprachschnipsel());
        // ein mitgeschickter Preis wird beim Einlesen verworfen
        assertTrue(mapper.writeValueAsString(auftrag.vorlage()).indexOf("preis") < 0);
    }

    @Test
    void korrektur_payload_wird_gemappt() throws Exception {
        String json = """
                {
                  "businessKey": "BK-200",
                  "strukturierteAngebotspositionen": {
                    "leistungen": [ {"bezeichnung": "Bodenfliesen", "beschreibung": "Feinsteinzeug", "menge": 15.0, "einheit": "m2"} ],
                    "material": [],
                    "notizen": ["bestehender Stand"]
                  },
                  "korrekturschnipsel": "Bitte zusaetzlich Sockelleisten einplanen."
                }
                """;
        AnalyseAuftrag auftrag = mapper.readValue(json, AiAnalysisRequest.class).toAuftrag(null);

        assertEquals(ProcessType.KORREKTUR, auftrag.typ());
        assertEquals("Bodenfliesen", auftrag.bestehendePositionen().leistungen().get(0).bezeichnung());
        assertEquals("Bitte zusaetzlich Sockelleisten einplanen.", auftrag.korrekturschnipsel());
    }

    @Test
    void leeres_json_wird_zu_null_feldern() throws Exception {
        AiAnalysisRequest req = mapper.readValue("{}", AiAnalysisRequest.class);
        assertNull(req.businessKey());
        assertNull(req.vorlage());
        assertNull(req.sprachschnipsel());
        assertNull(req.strukturierteAngebotspositionen());
        assertNull(req.korrekturschnipsel());
    }

    @Test
    void unbekannte_felder_brechen_nicht() throws Exception {
        String json = """
                { "businessKey": "BK-300", "vorlage": {}, "sprachschnipsel": "test",
                  "rawSnippet": "unbekannt", "customerId": 12345 }
                """;
        AiAnalysisRequest req = mapper.readValue(json, AiAnalysisRequest.class);
        assertEquals("BK-300", req.businessKey());
        assertTrue(req.vorlage().leistungen().isEmpty(), "Fehlende Listen werden zu leeren Listen.");
    }
}
