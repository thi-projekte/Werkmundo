package de.winfprojekt.werkmundo.aiservice.application.port;

import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import java.util.List;

/** Produktsuche im Katalog des Handwerkers fuer LLM-Call 2. */
public interface CatalogSearchPort {

    /**
     * Liefert bis zu {@code limit} Kandidaten, absteigend nach Relevanz. Bei keinem Treffer oder
     * technischem Fehler eine leere Liste (wird von Call 2 als "kein Treffer" behandelt).
     *
     * @param handwerkerId Keycloak-ID des Handwerkers, dessen Katalog durchsucht wird (kann {@code null} sein)
     */
    List<ProduktKandidat> search(String query, int limit, String handwerkerId);
}
