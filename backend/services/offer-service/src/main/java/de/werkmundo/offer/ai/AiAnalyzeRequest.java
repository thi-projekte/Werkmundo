package de.werkmundo.offer.ai;

import java.util.List;

/**
 * Payload für {@code POST /ai/analyze} (Erstangebot), siehe
 * {@code backend/services/ai-service/contracts/ai/AiAnalysisRequest.json}.
 * Enthält bewusst keine Kundendaten und keine Preise.
 */
public record AiAnalyzeRequest(String businessKey, Vorlage vorlage, String sprachschnipsel) {

    /** Leere Vorlage: der ai-service verlangt das Feld beim Erstangebot. */
    public record Vorlage(List<Object> leistungen, List<Object> material, List<String> notizen) {
        public static Vorlage leer() {
            return new Vorlage(List.of(), List.of(), List.of());
        }
    }

    public static AiAnalyzeRequest erstangebot(String businessKey, String sprachschnipsel) {
        return new AiAnalyzeRequest(businessKey, Vorlage.leer(), sprachschnipsel);
    }
}
