package de.werkmundo.invoice.application;

import de.werkmundo.invoice.domain.InvoicePositionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceCalculatorTest {

    @Test
    void calculatesPositionsAndNetTotal() {
        var positions = InvoiceCalculator.calculatePositions(List.of(
                input("Material", "2", "12.50"),
                input("Arbeitsleistung", "1.5", "60.00")
        ));

        assertEquals(new BigDecimal("25.00"),
                positions.get(0).positionsPreis());
        assertEquals(new BigDecimal("90.00"),
                positions.get(1).positionsPreis());
        assertEquals(new BigDecimal("115.00"),
                InvoiceCalculator.calculateNetTotal(positions));

        assertEquals(1, positions.get(0).reihenfolge());
        assertEquals(2, positions.get(1).reihenfolge());
    }

    @Test
    void roundsEachPositionBeforeAddingTheTotal() {
        var positions = InvoiceCalculator.calculatePositions(List.of(
                input("Erste Position", "0.5", "0.01"),
                input("Zweite Position", "0.5", "0.01")
        ));

        assertEquals(new BigDecimal("0.01"),
                positions.get(0).positionsPreis());
        assertEquals(new BigDecimal("0.01"),
                positions.get(1).positionsPreis());
        assertEquals(new BigDecimal("0.02"),
                InvoiceCalculator.calculateNetTotal(positions));
    }

    @Test
    void rejectsMissingPositions() {
        assertThrows(IllegalArgumentException.class,
                () -> InvoiceCalculator.calculatePositions(null));

        assertThrows(IllegalArgumentException.class,
                () -> InvoiceCalculator.calculatePositions(List.of()));
    }

    @Test
    void rejectsInvalidQuantities() {
        for (String quantity : List.of("0", "-1", "1.00001")) {
            assertThrows(IllegalArgumentException.class,
                    () -> InvoiceCalculator.calculatePositions(List.of(
                            input("Material", quantity, "12.50")
                    )), "Menge muss abgewiesen werden: " + quantity);
        }
    }

    @Test
    void rejectsInvalidUnitPrices() {
        for (String price : List.of("-0.01", "1.001", "10000000000000.00")) {
            assertThrows(IllegalArgumentException.class,
                    () -> InvoiceCalculator.calculatePositions(List.of(
                            input("Material", "1", price)
                    )), "Preis muss abgewiesen werden: " + price);
        }
    }

    @Test
    void rejectsMissingDescriptionUnitOrType() {
        var invalidInputs = List.of(
                new InvoicePositionInput(
                        null, " ", BigDecimal.ONE, "Stk.",
                        null, BigDecimal.ONE, InvoicePositionType.MATERIAL),
                new InvoicePositionInput(
                        null, "Material", BigDecimal.ONE, " ",
                        null, BigDecimal.ONE, InvoicePositionType.MATERIAL),
                new InvoicePositionInput(
                        null, "Material", BigDecimal.ONE, "Stk.",
                        null, BigDecimal.ONE, null)
        );

        for (var input : invalidInputs) {
            assertThrows(IllegalArgumentException.class,
                    () -> InvoiceCalculator.calculatePositions(List.of(input)));
        }
    }

    @Test
    void rejectsPositionTotalExceedingDatabaseCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> InvoiceCalculator.calculatePositions(List.of(
                        input("Material", "2", "9999999999999.99")
                )));
    }

    @Test
    void rejectsInvoiceTotalExceedingDatabaseCapacity() {
        var positions = InvoiceCalculator.calculatePositions(List.of(
                input("Erste Position", "1", "6000000000000.00"),
                input("Zweite Position", "1", "6000000000000.00")
        ));

        assertThrows(IllegalArgumentException.class,
                () -> InvoiceCalculator.calculateNetTotal(positions));
    }

    private InvoicePositionInput input(
            String description, String quantity, String unitPrice) {

        return new InvoicePositionInput(
                null,
                description,
                new BigDecimal(quantity),
                "Stk.",
                null,
                new BigDecimal(unitPrice),
                InvoicePositionType.MATERIAL
        );
    }
}