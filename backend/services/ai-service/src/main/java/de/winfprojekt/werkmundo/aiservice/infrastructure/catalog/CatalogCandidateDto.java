package de.winfprojekt.werkmundo.aiservice.infrastructure.catalog;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

/**
 * Ein Treffer der catalog-service-Suche (bzw. des Mock-Katalogs).
 *
 * <p><b>Preis-Stripping an der Grenze:</b> Kein Preisfeld - liefert der catalog-service Preise
 * mit, verwirft Jackson sie hier. Damit gelangen sie garantiert nicht in den LLM-Prompt.
 * {@code id} bleibt ein String (UUID im echten Katalog, Zahl im Mock).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogCandidateDto(
        String id,
        String articleNumber,
        String name,
        String description,
        String unit,
        @JsonAlias("categoryName") String category
) {

    public ProduktKandidat toDomain() {
        return new ProduktKandidat(id, articleNumber, name, description, unit, category);
    }
}
