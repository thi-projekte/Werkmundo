package de.winfprojekt.werkmundo.aiservice.infrastructure.catalog;

import de.winfprojekt.werkmundo.aiservice.application.port.CatalogSearchPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Waehlt zur Laufzeit die Implementierung von {@link CatalogSearchPort}:
 * {@code catalog.mock.enabled=true} (Default, lokal) → {@link MockCatalogSearch},
 * sonst → {@link RestCatalogSearch} (echter catalog-service).
 */
@ApplicationScoped
public class CatalogSearchConfiguration {

    private static final Logger LOG = Logger.getLogger(CatalogSearchConfiguration.class);

    @Produces
    @ApplicationScoped
    CatalogSearchPort catalogSearch(@ConfigProperty(name = "catalog.mock.enabled", defaultValue = "true") boolean mock,
                                    Instance<MockCatalogSearch> mockSearch,
                                    Instance<RestCatalogSearch> restSearch) {
        if (mock) {
            return mockSearch.get();
        }
        LOG.info("Katalogsuche ueber den catalog-service (catalog.mock.enabled=false).");
        return restSearch.get();
    }
}
