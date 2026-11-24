package de.werkmundo.offer.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Antwort von {@code POST /ai/analyze} (das {@code ergebnisKI}), siehe
 * {@code backend/services/ai-service/contracts/ai/AiAnalysisResult.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalyzeResult(
        Positionen strukturierteAngebotspositionen,
        List<String> korrekturvorschlaege,
        Double geschaetzteArbeitsdauerStunden
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Positionen(List<Position> leistungen, List<Position> material, List<String> notizen) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Position(String bezeichnung, String beschreibung, Double menge, String einheit,
                           String katalogProduktId) {
    }
}
