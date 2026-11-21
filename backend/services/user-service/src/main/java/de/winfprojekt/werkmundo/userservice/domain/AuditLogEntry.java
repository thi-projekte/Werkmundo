package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Eintrag im Änderungsprotokoll. Unveränderlich.
 *
 * <p>Die Beschreibung darf keine sensiblen Werte enthalten; IBANs nur über {@link Iban#mask(String)}.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntry {

    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 60, updatable = false)
    private String action;

    @Column(name = "description", length = MAX_DESCRIPTION_LENGTH, updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntry() {
        // für JPA
    }

    private AuditLogEntry(UUID companyId, UUID userId, String action, String description, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.companyId = DomainRules.requireValue(companyId, "Betrieb");
        this.userId = userId;
        this.action = DomainRules.requireText(action, "Aktion", 60);
        this.description = truncate(description);
        this.createdAt = DomainRules.requireValue(createdAt, "Zeitpunkt");
    }

    /**
     * @param action kurzer Code, z. B. {@code BANK_ACCOUNT_UPDATED}
     * @param userId auslösender Benutzer oder {@code null} bei Systemaktionen
     */
    public static AuditLogEntry record(UUID companyId, UUID userId, String action, String description) {
        return new AuditLogEntry(companyId, userId, action, description, Instant.now());
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= MAX_DESCRIPTION_LENGTH ? trimmed : trimmed.substring(0, MAX_DESCRIPTION_LENGTH);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
