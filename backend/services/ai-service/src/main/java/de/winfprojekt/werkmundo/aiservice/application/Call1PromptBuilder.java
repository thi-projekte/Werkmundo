package de.winfprojekt.werkmundo.aiservice.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.ProcessType;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Baut System-Prompt und User-Nachricht fuer LLM-Call 1.
 *
 * <p>Die User-Nachricht enthaelt nur den fachlich noetigen Ausschnitt als JSON:
 * Erstangebot = {@code sprachschnipsel} + {@code vorlage}, Korrektur =
 * {@code strukturierteAngebotspositionen} + {@code korrekturschnipsel}. {@code businessKey}
 * und {@code handwerkerId} gehen bewusst nicht an das Modell.
 */
@ApplicationScoped
public class Call1PromptBuilder {

    private static final String INTRO_ERSTANGEBOT =
            "Hier der Eingangs-Payload (so wie ihn die Process Engine schickt). "
                    + "Erzeuge daraus das geforderte JSON:";
    private static final String INTRO_KORREKTUR =
            "Hier der Eingangs-Payload (Korrektur). Ueberarbeite die bestehenden Positionen "
                    + "gemaess Korrekturschnipsel und gib das geforderte JSON zurueck:";

    private final ObjectMapper objectMapper;
    private final PromptTemplates prompts;

    public Call1PromptBuilder(ObjectMapper objectMapper, PromptTemplates prompts) {
        this.objectMapper = objectMapper;
        this.prompts = prompts;
    }

    public String systemPrompt(ProcessType type) {
        return type == ProcessType.KORREKTUR ? prompts.call1Korrektur() : prompts.call1Erstangebot();
    }

    public String userContent(AnalyseAuftrag auftrag) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String intro;
        if (auftrag.typ() == ProcessType.KORREKTUR) {
            payload.put("strukturierteAngebotspositionen", auftrag.bestehendePositionen());
            payload.put("korrekturschnipsel", auftrag.korrekturschnipsel());
            intro = INTRO_KORREKTUR;
        } else {
            payload.put("sprachschnipsel", auftrag.sprachschnipsel());
            payload.put("vorlage", auftrag.vorlage());
            intro = INTRO_ERSTANGEBOT;
        }

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            return intro + "\n\n" + json;
        } catch (JsonProcessingException e) {
            throw new LlmException("Konnte den Eingangs-Payload nicht zu JSON serialisieren.", e);
        }
    }
}
