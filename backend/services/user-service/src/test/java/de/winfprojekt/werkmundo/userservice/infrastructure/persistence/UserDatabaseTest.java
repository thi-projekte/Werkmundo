package de.winfprojekt.werkmundo.userservice.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.winfprojekt.werkmundo.userservice.domain.AuditLogEntry;
import de.winfprojekt.werkmundo.userservice.domain.AuditLogRepository;
import de.winfprojekt.werkmundo.userservice.domain.Company;
import de.winfprojekt.werkmundo.userservice.domain.CompanyFixtures;
import de.winfprojekt.werkmundo.userservice.domain.CompanyRepository;
import de.winfprojekt.werkmundo.userservice.domain.ConsentType;
import de.winfprojekt.werkmundo.userservice.domain.DetailLevel;
import de.winfprojekt.werkmundo.userservice.domain.Iban;
import de.winfprojekt.werkmundo.userservice.domain.TextTemplateType;
import de.winfprojekt.werkmundo.userservice.domain.ToneOfVoice;
import de.winfprojekt.werkmundo.userservice.domain.TravelCostData;
import de.winfprojekt.werkmundo.userservice.domain.TravelCostModel;
import de.winfprojekt.werkmundo.userservice.domain.User;
import de.winfprojekt.werkmundo.userservice.domain.UserRepository;
import de.winfprojekt.werkmundo.userservice.domain.UserRole;
import de.winfprojekt.werkmundo.userservice.domain.UserStatus;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prüft die user-db gegen die echten Flyway-Migrationen (H2 im PostgreSQL-Modus).
 * Startet die Anwendung, d. h. Flyway-Migration und Hibernate-Schemavalidierung laufen bei jedem Testlauf mit.
 */
@QuarkusTest
class UserDatabaseTest {

    @Inject
    CompanyRepository companies;

    @Inject
    UserRepository users;

    @Inject
    AuditLogRepository auditLogs;

    @Inject
    EntityManager entityManager;

    @Test
    void storesAndReloadsCompleteCompanyWithOwner() {
        String email = uniqueEmail();
        UUID[] ids = QuarkusTransaction.requiringNew().call(() -> {
            Company company = CompanyFixtures.completeForInvoices(new Company("Elektro Müller"));
            company.addEmployeeGroup("Meister", new BigDecimal("115.00"));
            company.configureTravelCosts(new TravelCostData(TravelCostModel.FLAT_PLUS_KM, new BigDecimal("25"), new BigDecimal("0.50"), 10));
            company.putTextTemplate(TextTemplateType.TERMS, "Es gelten unsere AGB.");
            company.getAiPreferences().update(ToneOfVoice.FRIENDLY, DetailLevel.DETAILED);
            companies.add(company);

            User owner = User.registerOwner(company, "kc-" + UUID.randomUUID(), email, "Max", "Müller");
            owner.giveConsent(ConsentType.TERMS, "2026-11", Instant.now());
            owner.giveConsent(ConsentType.PRIVACY, "2026-11", Instant.now());
            owner.recordLogin(Instant.now());
            owner.completeOnboarding();
            users.add(owner);
            return new UUID[] {company.getId(), owner.getId()};
        });

        QuarkusTransaction.requiringNew().run(() -> {
            User owner = users.findById(ids[1]).orElseThrow();
            assertEquals(UserRole.OWNER, owner.getRole());
            assertEquals(UserStatus.ACTIVE, owner.getStatus());
            assertTrue(owner.isOnboardingCompleted());
            assertEquals(2, owner.getConsents().size());
            assertEquals(owner.getId(), users.findByEmail(email.toUpperCase()).orElseThrow().getId());
            assertEquals(owner.getId(), users.findByKeycloakId(owner.getKeycloakId()).orElseThrow().getId());

            Company company = companies.findById(ids[0]).orElseThrow();
            assertEquals("Ingolstadt", company.getCity());
            assertEquals("DE89370400440532013000", company.defaultBankAccount().orElseThrow().getIban());
            assertEquals(List.of("Geselle", "Meister"),
                    company.getEmployeeGroups().stream().map(group -> group.getName()).toList());
            assertEquals(new BigDecimal("19.00"), company.getTaxInvoiceSettings().orElseThrow().getDefaultVatRate());
            assertEquals(TravelCostModel.FLAT_PLUS_KM, company.getTravelCostSettings().orElseThrow().getModel());
            assertEquals(ToneOfVoice.FRIENDLY, company.getAiPreferences().getToneOfVoice());
            assertEquals("Es gelten unsere AGB.", company.findTextTemplate(TextTemplateType.TERMS).orElseThrow().getContent());
            assertEquals(1, users.findByCompany(company.getId()).size());
        });
    }

    @Test
    void storesRegistrationData() {
        String email = uniqueEmail();
        String keycloakId = "kc-" + UUID.randomUUID();
        Instant acceptedAt = Instant.parse("2026-11-13T10:15:30Z");

        UUID userId = QuarkusTransaction.requiringNew().call(() -> {
            Company company = new Company("Sanitär Weber");
            companies.add(company);
            User owner = User.registerOwner(company, keycloakId, email, "Lena", "Weber");
            owner.giveConsent(ConsentType.TERMS, "2026-11", acceptedAt);
            owner.giveConsent(ConsentType.PRIVACY, "2026-11", acceptedAt);
            users.add(owner);
            return owner.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            User stored = users.findById(userId).orElseThrow();
            assertEquals("Lena", stored.getFirstName());
            assertEquals("Weber", stored.getLastName());
            assertEquals(email, stored.getEmail());
            assertEquals(keycloakId, stored.getKeycloakId());
            assertEquals(UserRole.OWNER, stored.getRole());
            assertEquals(UserStatus.PENDING, stored.getStatus());
            assertEquals("Sanitär Weber", stored.getCompany().getName());
            assertTrue(stored.hasConsented(ConsentType.TERMS, "2026-11"));
            assertTrue(stored.hasConsented(ConsentType.PRIVACY, "2026-11"));
            assertEquals(acceptedAt, stored.getConsents().get(0).getAcceptedAt());
        });
    }

    @Test
    void companyCanHaveAnyNumberOfEmployees() {
        int employeeCount = 25;
        UUID companyId = QuarkusTransaction.requiringNew().call(() -> {
            Company company = new Company("Dachdecker Huber");
            companies.add(company);
            users.add(User.registerOwner(company, "kc-" + UUID.randomUUID(), uniqueEmail(), "Josef", "Huber"));
            for (int i = 1; i <= employeeCount; i++) {
                users.add(User.addEmployee(company, "kc-" + UUID.randomUUID(), uniqueEmail(), "Mitarbeiter", "Nr. " + i));
            }
            return company.getId();
        });

        // Weitere Mitarbeiter lassen sich jederzeit später hinzufügen.
        QuarkusTransaction.requiringNew().run(() -> {
            Company company = companies.findById(companyId).orElseThrow();
            users.add(User.addEmployee(company, "kc-" + UUID.randomUUID(), uniqueEmail(), "Neu", "Eingestellt"));
        });

        QuarkusTransaction.requiringNew().run(() -> {
            List<User> team = users.findByCompany(companyId);
            assertEquals(employeeCount + 2, team.size());
            assertEquals(employeeCount + 1, team.stream().filter(user -> user.getRole() == UserRole.EMPLOYEE).count());
            assertEquals(1, team.stream().filter(user -> user.getRole() == UserRole.OWNER).count());
        });
    }

    @Test
    void companyCanHaveAnyNumberOfEmployeeGroups() {
        UUID companyId = QuarkusTransaction.requiringNew().call(() -> {
            Company company = new Company("Schreinerei Bauer");
            for (int i = 1; i <= 15; i++) {
                company.addEmployeeGroup("Lohngruppe " + i, new BigDecimal(50 + i));
            }
            companies.add(company);
            return company.getId();
        });

        QuarkusTransaction.requiringNew().run(() ->
                assertEquals(15, companies.findById(companyId).orElseThrow().getEmployeeGroups().size()));
    }

    @Test
    void changesToLoadedCompanyArePersisted() {
        UUID companyId = QuarkusTransaction.requiringNew().call(() -> {
            Company company = CompanyFixtures.completeForInvoices(new Company("Maler Schmidt"));
            companies.add(company);
            return company.getId();
        });

        UUID secondAccountId = QuarkusTransaction.requiringNew().call(() -> {
            Company company = companies.findById(companyId).orElseThrow();
            UUID id = company.addBankAccount("Anna Schmidt", "DE02120300000000202051", null, null).getId();
            company.markDefaultBankAccount(id);
            return id;
        });

        QuarkusTransaction.requiringNew().run(() -> {
            Company company = companies.findById(companyId).orElseThrow();
            assertEquals(2, company.getBankAccounts().size());
            assertEquals(secondAccountId, company.defaultBankAccount().orElseThrow().getId());
        });
    }

    @Test
    void emailMustBeUniqueAcrossUsers() {
        String email = uniqueEmail();
        QuarkusTransaction.requiringNew().run(() -> {
            Company company = new Company("Betrieb A");
            companies.add(company);
            users.add(User.registerOwner(company, "kc-" + UUID.randomUUID(), email, "Max", "Müller"));
        });

        assertTrue(QuarkusTransaction.requiringNew().call(() -> users.existsByEmail(email)));
        assertThrows(RuntimeException.class, () -> QuarkusTransaction.requiringNew().run(() -> {
            Company company = new Company("Betrieb B");
            companies.add(company);
            users.add(User.registerOwner(company, "kc-" + UUID.randomUUID(), email, "Erika", "Muster"));
        }));
    }

    @Test
    void databaseRejectsTaxSettingsWithoutIdentifier() {
        UUID companyId = QuarkusTransaction.requiringNew().call(() -> {
            Company company = new Company("Betrieb ohne Steuernummer");
            companies.add(company);
            return company.getId();
        });

        // Umgeht die Domänenprüfung absichtlich: auch die Datenbank selbst muss die Regel durchsetzen.
        assertThrows(RuntimeException.class, () -> QuarkusTransaction.requiringNew().run(() -> entityManager
                .createNativeQuery("insert into tax_invoice_settings (company_id, vat_treatment, default_vat_rate,"
                        + " invoice_prefix, payment_term_days, created_at, updated_at)"
                        + " values (:id, 'REGULAR', 19.00, 'RE-', 14, current_timestamp, current_timestamp)")
                .setParameter("id", companyId)
                .executeUpdate()));
    }

    @Test
    void auditLogStoresMaskedIbanOnly() {
        UUID companyId = QuarkusTransaction.requiringNew().call(() -> {
            Company company = new Company("Betrieb mit Protokoll");
            companies.add(company);
            auditLogs.add(AuditLogEntry.record(company.getId(), null, "BANK_ACCOUNT_ADDED",
                    "Konto " + Iban.mask("DE89370400440532013000") + " hinzugefügt"));
            return company.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            List<AuditLogEntry> entries = auditLogs.findLatestByCompany(companyId, 10);
            assertEquals(1, entries.size());
            assertEquals("Konto DE89 … 3000 hinzugefügt", entries.get(0).getDescription());
            assertFalse(entries.get(0).getDescription().contains("0532013000"));
        });
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.de";
    }
}
