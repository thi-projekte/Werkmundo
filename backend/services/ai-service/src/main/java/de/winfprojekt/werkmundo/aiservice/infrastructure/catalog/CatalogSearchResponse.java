package de.winfprojekt.werkmundo.aiservice.infrastructure.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Antwort von {@code GET /catalog/material/search}: Kandidaten absteigend nach Relevanz. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSearchResponse(List<CatalogCandidateDto> candidates) {}
