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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Anfahrtskosten eines Betriebs (1:1). Wird über {@link Company} verwaltet. */
@Entity
@Table(name = "travel_cost_settings")
public class TravelCostSettings extends AuditableEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "model", nullable = false, length = 20)
    private TravelCostModel model;

    @Column(name = "flat_rate", precision = 10, scale = 2)
    private BigDecimal flatRate;

    @Column(name = "km_rate", precision = 10, scale = 2)
    private BigDecimal kmRate;

    @Column(name = "free_km")
    private Integer freeKm;

    protected TravelCostSettings() {
        // für JPA
    }

    TravelCostSettings(Company company, TravelCostData data) {
        this.company = company;
        apply(data);
    }

    void apply(TravelCostData data) {
        DomainRules.requireValue(data, "Anfahrtskosten");
        TravelCostModel newModel = DomainRules.requireValue(data.model(), "Anfahrtsmodell");
        BigDecimal newFlatRate = DomainRules.optionalNonNegative(data.flatRate(), "Pauschale");
        BigDecimal newKmRate = DomainRules.optionalNonNegative(data.kmRate(), "Kilometersatz");
        if (newModel != TravelCostModel.KM_ONLY && newFlatRate == null) {
            throw new IllegalArgumentException("Für das Modell " + newModel + " ist eine Pauschale nötig");
        }
        if (newModel != TravelCostModel.FLAT && newKmRate == null) {
            throw new IllegalArgumentException("Für das Modell " + newModel + " ist ein Kilometersatz nötig");
        }
        if (data.freeKm() != null && data.freeKm() < 0) {
            throw new IllegalArgumentException("Freikilometer dürfen nicht negativ sein");
        }
        this.model = newModel;
        this.flatRate = newFlatRate == null ? null : newFlatRate.setScale(2, RoundingMode.HALF_UP);
        this.kmRate = newKmRate == null ? null : newKmRate.setScale(2, RoundingMode.HALF_UP);
        this.freeKm = data.freeKm();
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public Company getCompany() {
        return company;
    }

    public TravelCostModel getModel() {
        return model;
    }

    public BigDecimal getFlatRate() {
        return flatRate;
    }

    public BigDecimal getKmRate() {
        return kmRate;
    }

    public Integer getFreeKm() {
        return freeKm;
    }
}
