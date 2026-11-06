package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;
import de.winfprojekt.werkmundo.aiservice.domain.Position;
import de.winfprojekt.werkmundo.aiservice.domain.ProcessType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubResultGeneratorTest {

    private final StubResultGenerator stub = new StubResultGenerator();

    @Test
    void erstangebot_liefertWallboxMaterialUndVierStunden() {
        ErgebnisKi e = stub.generate(ProcessType.ERSTANGEBOT);
        assertEquals(4, e.strukturierteAngebotspositionen().material().size());
        assertEquals("Wallbox", e.strukturierteAngebotspositionen().material().get(0).bezeichnung());
        assertEquals(4.0, e.geschaetzteArbeitsdauerStunden());
        assertTrue(e.strukturierteAngebotspositionen().leistungen().isEmpty());
    }

    @Test
    void korrektur_ergaenztFiSchutzschalter() {
        ErgebnisKi e = stub.generate(ProcessType.KORREKTUR);
        assertEquals(5, e.strukturierteAngebotspositionen().material().size());
        assertTrue(e.strukturierteAngebotspositionen().material().stream()
                .anyMatch(p -> p.bezeichnung().startsWith("FI-Schutzschalter")));
    }

    @Test
    void stub_enthaeltKeineKatalogIds_undNurPositiveMengen() {
        for (Position p : stub.generate(ProcessType.ERSTANGEBOT).strukturierteAngebotspositionen().material()) {
            assertNull(p.katalogProduktId());
            assertTrue(p.menge() > 0);
        }
    }
}
