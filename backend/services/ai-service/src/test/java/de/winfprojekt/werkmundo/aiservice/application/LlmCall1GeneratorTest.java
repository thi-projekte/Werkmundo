package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.application.port.LlmPort;
import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.Angebotspositionen;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;
import de.winfprojekt.werkmundo.aiservice.domain.Vorlage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmCall1GeneratorTest {

    private LlmPort llm;
    private LlmCall1Generator generator;

    private static final AnalyseAuftrag ERSTANGEBOT = AnalyseAuftrag.of("BK-1", null,
            new Vorlage(List.of(), List.of(), List.of()), "Drei Steckdosen im Wohnzimmer setzen.", null, null);
    private static final AnalyseAuftrag KORREKTUR = AnalyseAuftrag.of("BK-2", null, null, null,
            new Angebotspositionen(List.of(), List.of(), List.of()), "Sockelleisten ergaenzen.");

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper();
        llm = Mockito.mock(LlmPort.class);
        generator = new LlmCall1Generator(llm, new Call1PromptBuilder(om, new PromptTemplates()),
                new ErgebnisKiParser(om), new StubResultGenerator());
    }

    @Test
    void ohneLlm_liefertStub_undRuftLlmNicht() {
        when(llm.isConfigured()).thenReturn(false);

        ErgebnisKi result = generator.generate(ERSTANGEBOT);

        assertEquals("Wallbox", result.strukturierteAngebotspositionen().material().get(0).bezeichnung());
        verify(llm, never()).complete(anyString(), anyString());
    }

    @Test
    void ohneLlm_korrektur_liefertKorrekturStub() {
        when(llm.isConfigured()).thenReturn(false);
        assertEquals(5, generator.generate(KORREKTUR).strukturierteAngebotspositionen().material().size());
    }

    @Test
    void mitLlm_parstAntwort() {
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenReturn("""
                {"strukturierteAngebotspositionen":{"leistungen":[],
                  "material":[{"bezeichnung":"Doppelsteckdose","beschreibung":"UP","menge":3,"einheit":"Stk"}],
                  "notizen":[]},"korrekturvorschlaege":[],"geschaetzteArbeitsdauerStunden":null}
                """);

        ErgebnisKi result = generator.generate(ERSTANGEBOT);

        assertEquals("Doppelsteckdose", result.strukturierteAngebotspositionen().material().get(0).bezeichnung());
        assertEquals(3.0, result.strukturierteAngebotspositionen().material().get(0).menge());
    }

    @Test
    void llmFehler_faelltAufStubZurueck() {
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenThrow(new LlmException("503"));

        assertEquals("Wallbox", generator.generate(ERSTANGEBOT)
                .strukturierteAngebotspositionen().material().get(0).bezeichnung());
    }

    @Test
    void unparsbareAntwort_faelltAufStubZurueck() {
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString())).thenReturn("Tut mir leid, das kann ich nicht.");

        assertEquals(4.0, generator.generate(ERSTANGEBOT).geschaetzteArbeitsdauerStunden());
    }
}
