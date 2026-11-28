package de.werkmundo.catalog.domain;

import java.util.List;

public interface MaterialRepository {
    void replace(String ownerId, List<Material> materials);
}
