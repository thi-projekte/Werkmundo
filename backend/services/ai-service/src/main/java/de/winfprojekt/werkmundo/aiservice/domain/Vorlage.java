package de.winfprojekt.werkmundo.aiservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

/**
 * Vorlage des Handwerkers: typische Leistungen, Materialien und Notizen. Die Process Engine
 * reicht sie beim Erstangebot an den ai-service durch; sie dient dem LLM als fachlicher Rahmen.
 * Fehlende Listen werden zu leeren Listen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Vorlage(
        List<Position> leistungen,
        List<Position> material,
        List<String> notizen
) {

    public Vorlage {
        leistungen = leistungen == null ? List.of() : leistungen.stream().filter(Objects::nonNull).toList();
        material = material == null ? List.of() : material.stream().filter(Objects::nonNull).toList();
        notizen = notizen == null ? List.of() : notizen.stream().filter(Objects::nonNull).toList();
    }
}
