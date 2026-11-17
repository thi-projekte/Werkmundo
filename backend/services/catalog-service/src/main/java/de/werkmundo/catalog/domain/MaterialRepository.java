package de.werkmundo.catalog.domain;

import java.util.List;

public interface MaterialRepository {
    void upsert(String ownerId, List<Material> materials);
}
