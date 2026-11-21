package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Nachweis, dass ein Benutzer einer bestimmten Fassung von AGB oder Datenschutzerklärung zugestimmt hat. */
@Entity
@Table(name = "consents")
public class Consent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private ConsentType type;

    @Column(name = "version", nullable = false, length = 20, updatable = false)
    private String version;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    protected Consent() {
        // für JPA
    }

    Consent(User user, ConsentType type, String version, Instant acceptedAt) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.type = DomainRules.requireValue(type, "Art der Zustimmung");
        this.version = DomainRules.requireText(version, "Version", 20);
        this.acceptedAt = DomainRules.requireValue(acceptedAt, "Zeitpunkt der Zustimmung");
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ConsentType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}
