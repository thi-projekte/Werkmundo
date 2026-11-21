package de.winfprojekt.werkmundo.userservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private User newOwner() {
        return User.registerOwner(new Company("Elektro Müller"), "kc-123", " Max@Example.DE ", "Max", "Müller");
    }

    @Test
    void registeredOwnerIsPendingWithNormalizedEmail() {
        User user = newOwner();
        assertEquals(UserRole.OWNER, user.getRole());
        assertEquals(UserStatus.PENDING, user.getStatus());
        assertEquals("max@example.de", user.getEmail());
        assertFalse(user.isOnboardingCompleted());
        assertThrows(IllegalArgumentException.class,
                () -> User.registerOwner(new Company("X"), "kc-1", "kein-email", "Max", "Müller"));
    }

    @Test
    void firstLoginActivatesAccount() {
        User user = newOwner();
        Instant now = Instant.parse("2026-11-13T09:00:00Z");
        user.recordLogin(now);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(now, user.getLastLoginAt());

        user.disable();
        assertThrows(IllegalStateException.class, () -> user.recordLogin(now));
        user.enable();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void onboardingNeedsCompleteCompanyData() {
        User user = newOwner();
        IllegalStateException error = assertThrows(IllegalStateException.class, user::completeOnboarding);
        assertTrue(error.getMessage().contains("Bankverbindung"));

        CompanyFixtures.completeForInvoices(user.getCompany());
        user.completeOnboarding();
        assertTrue(user.isOnboardingCompleted());
    }

    @Test
    void sameConsentVersionIsStoredOnce() {
        User user = newOwner();
        Instant now = Instant.now();
        user.giveConsent(ConsentType.TERMS, "2026-11", now);
        user.giveConsent(ConsentType.TERMS, "2026-11", now);
        user.giveConsent(ConsentType.PRIVACY, "2026-11", now);

        assertEquals(2, user.getConsents().size());
        assertTrue(user.hasConsented(ConsentType.TERMS, "2026-11"));
        assertFalse(user.hasConsented(ConsentType.TERMS, "2027-01"));
    }

    @Test
    void anonymizeRemovesPersonalData() {
        User user = newOwner();
        user.changePhone("0841 123456");
        user.anonymize();

        assertEquals(UserStatus.DELETED, user.getStatus());
        assertEquals("Gelöscht", user.getFirstName());
        assertTrue(user.getEmail().endsWith("@deleted.invalid"));
        assertNull(user.getPhone());
        assertThrows(IllegalStateException.class, () -> user.recordLogin(Instant.now()));
    }
}
