package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.application.port.LlmPort;
import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * LLM-Call 1 (Extraktion): Sprachschnipsel bzw. Korrektur → strukturierte Angebotspositionen.
 *
 * <p>Ablauf: Prompt bauen ({@link Call1PromptBuilder}) → LLM aufrufen ({@link LlmPort}) →
 * Antwort parsen ({@link ErgebnisKiParser}). Ohne konfiguriertes LLM oder bei einem Fehler
 * wird das Ergebnis des {@link StubResultGenerator} geliefert (als WARN/ERROR geloggt).
 */
@ApplicationScoped
public class LlmCall1Generator {

    private static final Logger LOG = Logger.getLogger(LlmCall1Generator.class);

    private final LlmPort llm;
    private final Call1PromptBuilder promptBuilder;
    private final ErgebnisKiParser parser;
    private final StubResultGenerator stub;

    public LlmCall1Generator(LlmPort llm,
                             Call1PromptBuilder promptBuilder,
                             ErgebnisKiParser parser,
                             StubResultGenerator stub) {
        this.llm = llm;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.stub = stub;
    }

    public ErgebnisKi generate(AnalyseAuftrag auftrag) {
        if (!llm.isConfigured()) {
            LOG.warnf("Kein LLM konfiguriert (MEGALLM_API_KEY fehlt) - Stub-Fallback (%s, businessKey=%s).",
                    auftrag.typ(), auftrag.businessKey());
            return stub.generate(auftrag.typ());
        }

        try {
            String raw = llm.complete(
                    promptBuilder.systemPrompt(auftrag.typ()),
                    promptBuilder.userContent(auftrag));
            ErgebnisKi ergebnis = parser.parse(raw);
            LOG.infof("LLM-Call 1 erfolgreich (%s, businessKey=%s).", auftrag.typ(), auftrag.businessKey());
            return ergebnis;
        } catch (RuntimeException e) {
            LOG.errorf(e, "LLM-Call 1 fehlgeschlagen - Stub-Fallback (%s, businessKey=%s).",
                    auftrag.typ(), auftrag.businessKey());
            return stub.generate(auftrag.typ());
        }
    }
}
