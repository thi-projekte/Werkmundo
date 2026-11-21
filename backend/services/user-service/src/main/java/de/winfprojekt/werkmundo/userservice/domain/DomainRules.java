package de.winfprojekt.werkmundo.userservice.domain;

import java.math.BigDecimal;
import java.util.Locale;

/** Kleine Prüf- und Normalisierungshilfen für die Domänenklassen. */
final class DomainRules {

    private DomainRules() {
    }

    /** Pflichttext: darf nicht leer sein, wird getrimmt und auf die Spaltenlänge geprüft. */
    static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " darf nicht leer sein");
        }
        return checkLength(value.trim(), field, maxLength);
    }

    /** Optionaler Text: leere Eingaben werden zu null. */
    static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return checkLength(value.trim(), field, maxLength);
    }

    static String normalizeEmail(String value, String field) {
        String email = requireText(value, field, 254).toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException(field + " ist keine gültige E-Mail-Adresse");
        }
        return email;
    }

    static String optionalEmail(String value, String field) {
        return value == null || value.isBlank() ? null : normalizeEmail(value, field);
    }

    static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " fehlt");
        }
        return optionalNonNegative(value, field);
    }

    static BigDecimal optionalNonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(field + " darf nicht negativ sein");
        }
        return value;
    }

    static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " fehlt");
        }
        return value;
    }

    private static String checkLength(String value, String field, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " ist zu lang (maximal " + maxLength + " Zeichen)");
        }
        return value;
    }
}
