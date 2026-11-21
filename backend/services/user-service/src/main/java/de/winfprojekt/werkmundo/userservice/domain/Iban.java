package de.winfprojekt.werkmundo.userservice.domain;

import java.math.BigInteger;
import java.util.Locale;

/** Prüfung und Darstellung von IBANs nach ISO 13616 (Prüfsumme Modulo 97). */
public final class Iban {

    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

    private Iban() {
    }

    /** Entfernt Leerzeichen und wandelt in Großbuchstaben um. */
    public static String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        String iban = normalize(value);
        if (iban == null || !iban.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$")) {
            return false;
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder digits = new StringBuilder(rearranged.length() * 2);
        for (char c : rearranged.toCharArray()) {
            digits.append(Character.isDigit(c) ? String.valueOf(c) : String.valueOf(c - 'A' + 10));
        }
        return new BigInteger(digits.toString()).mod(NINETY_SEVEN).intValue() == 1;
    }

    /** Normalisiert und prüft eine IBAN; wirft bei ungültiger Eingabe. */
    public static String requireValid(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("IBAN ist ungültig");
        }
        return normalize(value);
    }

    /** Gekürzte Darstellung für Protokolle, z. B. "DE89 … 3000". */
    public static String mask(String value) {
        String iban = normalize(value);
        if (iban == null || iban.length() < 8) {
            return "****";
        }
        return iban.substring(0, 4) + " … " + iban.substring(iban.length() - 4);
    }
}
