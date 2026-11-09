package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErgebnisKiParserTest {

    private final ErgebnisKiParser parser = new ErgebnisKiParser(new ObjectMapper());

    @Test
    void parstSauberesJson() {
        ErgebnisKi result = parser.parse("""
                {"strukturierteAngebotspositionen":{
                  "leistungen":[{"bezeichnung":"Fliesen verlegen","beschreibung":"Bad","menge":15,"einheit":"m²"}],
                  "material":[],
                  "notizen":["Vor Ort pruefen"]
                },"korrekturvorschlaege":["Fugenfarbe abstimmen"]}
                """);
        assertEquals("Fliesen verlegen", result.strukturierteAngebotspositionen().leistungen().get(0).bezeichnung());
        assertEquals(15.0, result.strukturierteAngebotspositionen().leistungen().get(0).menge());
        assertEquals(1, result.strukturierteAngebotspositionen().notizen().size());
        assertEquals(1, result.korrekturvorschlaege().size());
    }

    @Test
    void entferntMarkdownFences() {
        ErgebnisKi result = parser.parse("""
                ```json
                {"strukturierteAngebotspositionen":{"leistungen":[],"material":[],"notizen":[]},"korrekturvorschlaege":[]}
                ```
                """);
        assertTrue(result.strukturierteAngebotspositionen().leistungen().isEmpty());
    }

    @Test
    void normalisiertFehlendenWrapper() {
        ErgebnisKi result = parser.parse("""
                {"leistungen":[{"bezeichnung":"Steckdose","beschreibung":"UP","menge":3,"einheit":"Stk"}],
                 "material":[],"notizen":[]}
                """);
        assertEquals(3.0, result.strukturierteAngebotspositionen().leistungen().get(0).menge());
        assertTrue(result.korrekturvorschlaege().isEmpty());
    }

    @Test
    void uebernimmtNullMenge() {
        ErgebnisKi result = parser.parse("""
                {"strukturierteAngebotspositionen":{
                  "leistungen":[{"bezeichnung":"X","beschreibung":"Y","menge":null,"einheit":"Stk"}],
                  "material":[],"notizen":[]},"korrekturvorschlaege":[]}
                """);
        assertNull(result.strukturierteAngebotspositionen().leistungen().get(0).menge());
    }

    @Test
    void ungueltigesJson_wirftLlmException() {
        assertThrows(LlmException.class, () -> parser.parse("das ist kein JSON"));
    }

    @Test
    void jsonArrayStattObjekt_wirftLlmException() {
        assertThrows(LlmException.class, () -> parser.parse("[1,2,3]"));
    }

    @Test
    void parstGeschaetzteArbeitsdauerStunden() {
        assertEquals(2.5, parser.parse("""
                {"strukturierteAngebotspositionen":{"leistungen":[],"material":[],"notizen":[]},
                 "korrekturvorschlaege":[],"geschaetzteArbeitsdauerStunden":2.5}
                """).geschaetzteArbeitsdauerStunden());
    }

    @Test
    void ohneArbeitsdauer_istNull() {
        assertNull(parser.parse("""
                {"strukturierteAngebotspositionen":{"leistungen":[],"material":[],"notizen":[]},"korrekturvorschlaege":[]}
                """).geschaetzteArbeitsdauerStunden());
    }
}
