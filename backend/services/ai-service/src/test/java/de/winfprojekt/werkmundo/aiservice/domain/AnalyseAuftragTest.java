package de.winfprojekt.werkmundo.aiservice.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fachregel zur Erkennung Erstangebot / Korrektur (frueher ProcessTypeDetector). */
class AnalyseAuftragTest {

    private static final Vorlage VORLAGE = new Vorlage(List.of(), List.of(), List.of());
    private static final Angebotspositionen POSITIONEN = new Angebotspositionen(List.of(), List.of(), List.of());

    @Test
    void erstangebot_wennVorlageUndSprachschnipsel() {
        AnalyseAuftrag a = AnalyseAuftrag.of("BK-1", "hw-1", VORLAGE, "Wallbox installieren", null, null);
        assertEquals(ProcessType.ERSTANGEBOT, a.typ());
        assertEquals("hw-1", a.handwerkerId());
        assertNull(a.korrekturschnipsel());
    }

    @Test
    void korrektur_wennPositionenUndKorrekturschnipsel() {
        AnalyseAuftrag a = AnalyseAuftrag.of("BK-2", null, null, null, POSITIONEN, "Sockelleisten ergaenzen");
        assertEquals(ProcessType.KORREKTUR, a.typ());
        assertNull(a.vorlage());
    }

    @Test
    void korrektur_hatVorrang_wennBeidesGesetzt() {
        AnalyseAuftrag a = AnalyseAuftrag.of("BK-3", null, VORLAGE, "text", POSITIONEN, "korrektur");
        assertEquals(ProcessType.KORREKTUR, a.typ());
        assertNull(a.sprachschnipsel(), "Erstangebot-Felder werden bei Korrektur nicht mitgefuehrt.");
    }

    @Test
    void unvollstaendigesErstangebot_wirdAbgelehnt() {
        assertThrows(UngueltigerAuftragException.class,
                () -> AnalyseAuftrag.of("BK-4", null, VORLAGE, null, null, null));
    }

    @Test
    void unvollstaendigeKorrektur_wirdAbgelehnt() {
        assertThrows(UngueltigerAuftragException.class,
                () -> AnalyseAuftrag.of("BK-5", null, null, null, POSITIONEN, null));
    }

    @Test
    void ohneBusinessKey_wirdAbgelehnt() {
        UngueltigerAuftragException e = assertThrows(UngueltigerAuftragException.class,
                () -> AnalyseAuftrag.of(" ", null, VORLAGE, "text", null, null));
        assertTrue(e.getMessage().contains("businessKey"));
    }
}
