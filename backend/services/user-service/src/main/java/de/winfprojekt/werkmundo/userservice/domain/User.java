package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Benutzerkonto eines Betriebs.
 *
 * <p>Das Passwort liegt ausschließlich in Keycloak. Die Verbindung erfolgt über {@link #getKeycloakId()},
 * den Claim {@code sub} des Access-Tokens.
 */
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @Column(name = "keycloak_id", nullable = false, length = 64)
    private String keycloakId;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Consent> consents = new ArrayList<>();

    protected User() {
        // für JPA
    }

    public User(Company company, String keycloakId, String email, String firstName, String lastName, UserRole role) {
        this.id = UUID.randomUUID();
        this.company = DomainRules.requireValue(company, "Betrieb");
        this.keycloakId = DomainRules.requireText(keycloakId, "Keycloak-ID", 64);
        this.role = DomainRules.requireValue(role, "Rolle");
        this.status = UserStatus.PENDING;
        changeEmail(email);
        changeName(firstName, lastName);
    }

    /** Registrierung eines neuen Inhabers zusammen mit seinem Betrieb. */
    public static User registerOwner(Company company, String keycloakId, String email, String firstName, String lastName) {
        return new User(company, keycloakId, email, firstName, lastName, UserRole.OWNER);
    }

    /** Legt einen Mitarbeiter im Betrieb an. Pro Betrieb sind beliebig viele Mitarbeiter möglich. */
    public static User addEmployee(Company company, String keycloakId, String email, String firstName, String lastName) {
        return new User(company, keycloakId, email, firstName, lastName, UserRole.EMPLOYEE);
    }

    // ---------------------------------------------------------------- Lebenszyklus

    /** Erster und jeder weitere Login. Ein neues Konto wird dabei aktiviert. */
    public void recordLogin(Instant now) {
        ensureNotDeleted();
        if (status == UserStatus.DISABLED) {
            throw new IllegalStateException("Konto ist gesperrt");
        }
        this.lastLoginAt = DomainRules.requireValue(now, "Zeitpunkt");
        if (status == UserStatus.PENDING) {
            status = UserStatus.ACTIVE;
        }
    }

    /** Schließt das Onboarding ab, sofern alle Rechnungsangaben des Betriebs vollständig sind. */
    public void completeOnboarding() {
        ensureNotDeleted();
        List<String> missing = company.missingInvoiceRequirements();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Onboarding unvollständig, es fehlt: " + String.join(", ", missing));
        }
        this.onboardingCompleted = true;
    }

    public void disable() {
        ensureNotDeleted();
        status = UserStatus.DISABLED;
    }

    public void enable() {
        ensureNotDeleted();
        status = lastLoginAt == null ? UserStatus.PENDING : UserStatus.ACTIVE;
    }

    /**
     * Löscht das Konto DSGVO-konform: persönliche Daten werden überschrieben, der Datensatz bleibt
     * für Protokoll-Verweise erhalten. Das Keycloak-Konto entfernt der Service separat.
     */
    public void anonymize() {
        String placeholder = "deleted-" + id;
        this.keycloakId = placeholder;
        this.email = placeholder + "@deleted.invalid";
        this.firstName = "Gelöscht";
        this.lastName = "Gelöscht";
        this.phone = null;
        this.profilePictureUrl = null;
        this.status = UserStatus.DELETED;
    }

    // ---------------------------------------------------------------- Profil

    public void changeName(String firstName, String lastName) {
        this.firstName = DomainRules.requireText(firstName, "Vorname", 100);
        this.lastName = DomainRules.requireText(lastName, "Nachname", 100);
    }

    public void changeEmail(String email) {
        this.email = DomainRules.normalizeEmail(email, "E-Mail");
    }

    public void changePhone(String phone) {
        this.phone = DomainRules.optionalText(phone, "Telefon", 40);
    }

    public void changeProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = DomainRules.optionalText(profilePictureUrl, "Profilbild-URL", 500);
    }

    public void changeRole(UserRole role) {
        this.role = DomainRules.requireValue(role, "Rolle");
    }

    // ---------------------------------------------------------------- Zustimmungen

    /** Speichert eine Zustimmung. Dieselbe Version wird nicht doppelt gespeichert. */
    public void giveConsent(ConsentType type, String version, Instant acceptedAt) {
        if (!hasConsented(type, version)) {
            consents.add(new Consent(this, type, version, acceptedAt));
        }
    }

    public boolean hasConsented(ConsentType type, String version) {
        return consents.stream().anyMatch(consent -> consent.getType() == type && consent.getVersion().equals(version));
    }

    private void ensureNotDeleted() {
        if (status == UserStatus.DELETED) {
            throw new IllegalStateException("Konto wurde gelöscht");
        }
    }

    // ---------------------------------------------------------------- Getter

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public List<Consent> getConsents() {
        return Collections.unmodifiableList(consents);
    }
}
