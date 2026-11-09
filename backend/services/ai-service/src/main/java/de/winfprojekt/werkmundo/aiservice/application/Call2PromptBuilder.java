package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.domain.Position;
import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Baut System-Prompt und User-Nachricht fuer LLM-Call 2 (Produktauswahl). Das Parsen der
 * Antwort liegt im {@link Call2AnswerParser}.
 *
 * <p>Die Kandidaten werden <b>neutral nach articleNumber sortiert</b>, damit das Modell nicht
 * einfach den ersten Suchtreffer uebernimmt. Preise sind nicht enthalten.
 */
@ApplicationScoped
public class Call2PromptBuilder {

    private final PromptTemplates prompts;

    public Call2PromptBuilder(PromptTemplates prompts) {
        this.prompts = prompts;
    }

    public String systemPrompt() {
        return prompts.call2Produktauswahl();
    }

    public String userContent(Position position, List<ProduktKandidat> kandidaten) {
        List<ProduktKandidat> neutral = new ArrayList<>(kandidaten);
        neutral.sort(Comparator.comparing(c -> c.articleNumber() == null ? "" : c.articleNumber()));

        StringBuilder sb = new StringBuilder();
        sb.append("POSITION:\n")
                .append("  Bezeichnung: ").append(nz(position.bezeichnung())).append('\n')
                .append("  Beschreibung: ").append(position.beschreibung() == null || position.beschreibung().isBlank()
                        ? "(keine)" : position.beschreibung()).append('\n')
                .append("  Menge: ").append(position.menge() == null ? "?" : position.menge())
                .append(' ').append(nz(position.einheit())).append("\n\n")
                .append("KANDIDATEN (").append(neutral.size()).append("):\n");
        for (ProduktKandidat k : neutral) {
            sb.append("- ").append(nz(k.articleNumber())).append(" | ").append(nz(k.name()))
                    .append(" | ").append(nz(k.description())).append(" | Einheit: ").append(nz(k.unit()))
                    .append('\n');
        }
        sb.append("\nWaehle den passenden articleNumber oder \"")
                .append(Call2AnswerParser.KEIN_TREFFER).append("\". Nur JSON.");
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
