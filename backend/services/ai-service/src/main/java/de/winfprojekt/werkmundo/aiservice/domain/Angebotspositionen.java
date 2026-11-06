package de.winfprojekt.werkmundo.aiservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

/**
 * Strukturierte Angebotspositionen eines konkreten Falls.
 *
 * <pre>
 * { "leistungen": [Position], "material": [Position], "notizen": ["..."] }
 * </pre>
 *
 * <p>Eingang bei der Korrektur (bestehender Stand) und Ausgang in jedem Fall (Feld
 * {@code strukturierteAngebotspositionen} von {@link ErgebnisKi}). Strukturell gleich wie die
 * {@link Vorlage}, fachlich aber etwas anderes (Vorlage = Stammdaten des Handwerkers).
 * JSON-Schema: {@code contracts/ai/AiAnalysisResult.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Angebotspositionen(
        List<Position> leistungen,
        List<Position> material,
        List<String> notizen
) {

    public Angebotspositionen {
        leistungen = leistungen == null ? List.of() : leistungen.stream().filter(Objects::nonNull).toList();
        material = material == null ? List.of() : material.stream().filter(Objects::nonNull).toList();
        notizen = notizen == null ? List.of() : notizen.stream().filter(Objects::nonNull).toList();
    }

    /** Kopie mit ersetzten Materialpositionen (z.B. nach LLM-Call 2). */
    public Angebotspositionen withMaterial(List<Position> neuesMaterial) {
        return new Angebotspositionen(leistungen, neuesMaterial, notizen);
    }
}
