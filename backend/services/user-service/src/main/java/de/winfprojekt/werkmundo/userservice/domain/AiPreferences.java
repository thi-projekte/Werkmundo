package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** KI-Einstellungen eines Betriebs (1:1). Wird beim Anlegen des Betriebs mit Standardwerten erzeugt. */
@Entity
@Table(name = "ai_preferences")
public class AiPreferences extends AuditableEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tone_of_voice", nullable = false, length = 20)
    private ToneOfVoice toneOfVoice = ToneOfVoice.FORMAL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "detail_level", nullable = false, length = 20)
    private DetailLevel detailLevel = DetailLevel.NORMAL;

    protected AiPreferences() {
        // für JPA
    }

    AiPreferences(Company company) {
        this.company = company;
    }

    public void update(ToneOfVoice toneOfVoice, DetailLevel detailLevel) {
        this.toneOfVoice = DomainRules.requireValue(toneOfVoice, "Tonfall");
        this.detailLevel = DomainRules.requireValue(detailLevel, "Detailgrad");
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public Company getCompany() {
        return company;
    }

    public ToneOfVoice getToneOfVoice() {
        return toneOfVoice;
    }

    public DetailLevel getDetailLevel() {
        return detailLevel;
    }
}
