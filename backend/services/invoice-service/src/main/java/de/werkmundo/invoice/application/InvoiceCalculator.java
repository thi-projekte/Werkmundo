package de.werkmundo.invoice.application;

import de.werkmundo.invoice.domain.InvoicePosition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class InvoiceCalculator {

    private InvoiceCalculator() {
    }

    public static List<InvoicePosition> calculatePositions(
            List<InvoicePositionInput> inputs) {

        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Mindestens eine Rechnungsposition ist erforderlich");
        }

        List<InvoicePosition> positions = new ArrayList<>();

        for (int index = 0; index < inputs.size(); index++) {
            InvoicePositionInput input = inputs.get(index);

            if (input == null) {
                throw new IllegalArgumentException(
                        "Eine Rechnungsposition darf nicht fehlen");
            }

            String description = requiredText(
                    input.bezeichnung(), "Bezeichnung");
            String unit = requiredText(input.einheit(), "Einheit");

            if (input.type() == null) {
                throw new IllegalArgumentException(
                        "Der Positionstyp darf nicht fehlen");
            }

            BigDecimal quantity = validateQuantity(input.menge());
            BigDecimal unitPrice = validateMoney(input.einzelPreis());

            BigDecimal positionPrice = validateMoney(
                    quantity.multiply(unitPrice)
                            .setScale(2, RoundingMode.HALF_UP)
            );

            positions.add(new InvoicePosition(
                    input.hersteller(),
                    description,
                    quantity,
                    unit,
                    input.katalogProduktId(),
                    unitPrice,
                    positionPrice,
                    index + 1,
                    input.type()
            ));
        }

        return List.copyOf(positions);
    }

    public static BigDecimal calculateNetTotal(
            List<InvoicePosition> positions) {

        BigDecimal total = positions.stream()
                .map(InvoicePosition::positionsPreis)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);

        return validateMoney(total);
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " darf nicht leer sein");
        }

        return value.strip();
    }

    private static BigDecimal validateQuantity(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Die Menge muss größer als null sein");
        }

        BigDecimal quantity;

        try {
            quantity = value.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Die Menge darf höchstens vier Nachkommastellen haben",
                    exception);
        }

        if (quantity.precision() > 38) {
            throw new IllegalArgumentException("Die Menge ist zu groß");
        }

        return quantity;
    }

    private static BigDecimal validateMoney(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    "Ein Geldbetrag darf nicht fehlen oder negativ sein");
        }

        BigDecimal amount;

        try {
            amount = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Ein Geldbetrag darf höchstens zwei Nachkommastellen haben",
                    exception);
        }

        if (amount.precision() > 15) {
            throw new IllegalArgumentException("Der Geldbetrag ist zu groß");
        }

        return amount;
    }
}