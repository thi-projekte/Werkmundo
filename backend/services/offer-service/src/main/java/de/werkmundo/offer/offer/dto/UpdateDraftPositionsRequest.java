package de.werkmundo.offer.offer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpdateDraftPositionsRequest(
        @NotNull
        List<@NotNull @Valid Position> positions
) {

    public record Position(
            @NotBlank
            String bezeichnung,

            @NotNull
            @DecimalMin(value = "0", inclusive = false)
            @Digits(integer = 34, fraction = 4)
            BigDecimal menge,

            @NotBlank
            String einheit,

            @NotNull
            @DecimalMin("0")
            @Digits(integer = 13, fraction = 2)
            BigDecimal einzelPreis
    ) {
    }
}