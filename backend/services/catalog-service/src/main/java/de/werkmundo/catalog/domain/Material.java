package de.werkmundo.catalog.domain;

import java.math.BigDecimal;

public record Material(String articleNumber, String name, String manufacturer,
                       String description, String category, String unit, BigDecimal price,
                       String currency) {
}
