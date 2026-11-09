package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.Angebotspositionen;
import de.winfprojekt.werkmundo.aiservice.domain.ProcessType;
import de.winfprojekt.werkmundo.aiservice.domain.Vorlage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Call1PromptBuilderTest {

    private final Call1PromptBuilder builder = new Call1PromptBuilder(new ObjectMapper(), new PromptTemplates());

    @Test
    void systemPromptErstangebot_verbietetPreise() {
        assertTrue(builder.systemPrompt(ProcessType.ERSTANGEBOT).contains("NIEMALS Preise"));
    }

    @Test
    void systemPromptKorrektur_unterscheidetSichVomErstangebot() {
        String erst = builder.systemPrompt(ProcessType.ERSTANGEBOT);
        String korr = builder.systemPrompt(ProcessType.KORREKTUR);
        assertNotEquals(erst, korr);
        assertTrue(korr.contains("ueberarbeitest"));
        assertTrue(korr.contains("NIEMALS Preise"));
    }

    @Test
    void systemPrompt_enthaeltArbeitsdauerRegel() {
        String erst = builder.systemPrompt(ProcessType.ERSTANGEBOT);
        assertTrue(erst.contains("geschaetzteArbeitsdauerStunden"));
        assertTrue(erst.contains("NIEMALS selbst"));
        assertTrue(builder.systemPrompt(ProcessType.KORREKTUR).contains("geschaetzteArbeitsdauerStunden"));
    }

    @Test
    void userContentErstangebot_ohneBusinessKeyUndHandwerkerId() {
        AnalyseAuftrag auftrag = AnalyseAuftrag.of("BK-GEHEIM-123", "hw-geheim",
                new Vorlage(List.of(), List.of(), List.of()), "Im Bad neue Bodenfliesen verlegen.", null, null);

        String user = builder.userContent(auftrag);

        assertTrue(user.contains("Im Bad neue Bodenfliesen verlegen."));
        assertTrue(user.contains("sprachschnipsel"));
        assertTrue(user.contains("vorlage"));
        assertFalse(user.contains("BK-GEHEIM-123"), "businessKey darf nicht an das Modell gehen.");
        assertFalse(user.contains("hw-geheim"), "handwerkerId darf nicht an das Modell gehen.");
    }

    @Test
    void userContentKorrektur_enthaeltKorrekturschnipsel() {
        AnalyseAuftrag auftrag = AnalyseAuftrag.of("BK-2", null, null, null,
                new Angebotspositionen(List.of(), List.of(), List.of()), "Bitte zusaetzlich Sockelleisten einplanen.");

        String user = builder.userContent(auftrag);

        assertTrue(user.contains("Bitte zusaetzlich Sockelleisten einplanen."));
        assertTrue(user.contains("korrekturschnipsel"));
        assertTrue(user.contains("strukturierteAngebotspositionen"));
    }
}
