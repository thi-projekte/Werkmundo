package de.winfprojekt.werkmundo.userservice.domain;

import java.math.BigDecimal;

/** Eingabewerte für {@link TravelCostSettings}; geprüft wird beim Übernehmen. */
public record TravelCostData(TravelCostModel model, BigDecimal flatRate, BigDecimal kmRate, Integer freeKm) {
}
