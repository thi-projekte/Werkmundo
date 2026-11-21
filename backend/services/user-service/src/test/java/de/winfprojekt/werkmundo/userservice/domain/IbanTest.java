package de.winfprojekt.werkmundo.userservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IbanTest {

    // Offizielle Beispiel-IBAN, kein echtes Konto.
    private static final String EXAMPLE = "DE89 3704 0044 0532 0130 00";

    @Test
    void acceptsValidIbanWithSpacesAndLowercase() {
        assertTrue(Iban.isValid(EXAMPLE));
        assertTrue(Iban.isValid("de89370400440532013000"));
        assertEquals("DE89370400440532013000", Iban.requireValid(EXAMPLE));
    }

    @Test
    void rejectsWrongChecksumAndGarbage() {
        assertFalse(Iban.isValid("DE88370400440532013000"));
        assertFalse(Iban.isValid("DE89"));
        assertFalse(Iban.isValid(null));
        assertFalse(Iban.isValid("1234567890123456"));
        assertThrows(IllegalArgumentException.class, () -> Iban.requireValid("DE00123"));
    }

    @Test
    void masksForAuditLog() {
        assertEquals("DE89 … 3000", Iban.mask(EXAMPLE));
        assertEquals("****", Iban.mask("DE1"));
    }
}
