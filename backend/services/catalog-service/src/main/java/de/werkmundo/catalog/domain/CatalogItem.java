package de.werkmundo.catalog.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItem(UUID id, String articleNumber, String name, String manufacturer,
                          String description, String category, String unit, BigDecimal price, String currency) {}
