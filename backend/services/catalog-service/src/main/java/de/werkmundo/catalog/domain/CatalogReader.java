package de.werkmundo.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogReader {
    List<CatalogItem> search(String ownerId, String query, int limit);
    Optional<CatalogItem> find(String ownerId, UUID id);
    long count(String ownerId);
}
