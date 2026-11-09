package de.winfprojekt.werkmundo.aiservice.infrastructure.catalog;

import de.winfprojekt.werkmundo.aiservice.application.port.CatalogSearchPort;
import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Objects;

/**
 * Produktsuche ueber den echten catalog-service. Aktiv mit {@code CATALOG_MOCK_ENABLED=false}.
 * Fehler werden als "kein Treffer" behandelt (leere Liste), damit die Verarbeitung weiterlaeuft.
 */
@ApplicationScoped
@Typed(RestCatalogSearch.class)
public class RestCatalogSearch implements CatalogSearchPort {

    private static final Logger LOG = Logger.getLogger(RestCatalogSearch.class);

    private final CatalogSearchClient client;

    public RestCatalogSearch(@RestClient CatalogSearchClient client) {
        this.client = client;
    }

    @Override
    public List<ProduktKandidat> search(String query, int limit, String handwerkerId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            CatalogSearchResponse response = client.search(query, limit, handwerkerId);
            if (response == null || response.candidates() == null) {
                return List.of();
            }
            return response.candidates().stream()
                    .filter(Objects::nonNull)
                    .map(CatalogCandidateDto::toDomain)
                    .toList();
        } catch (RuntimeException e) {
            LOG.warnf("catalog-service-Suche fehlgeschlagen, behandle als 'kein Treffer': %s", e.getMessage());
            return List.of();
        }
    }
}
