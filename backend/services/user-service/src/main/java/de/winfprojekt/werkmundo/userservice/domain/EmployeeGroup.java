package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/** Lohngruppe (z. B. Geselle, Meister) mit Netto-Stundensatz in Euro. Wird über {@link Company} verwaltet. */
@Entity
@Table(name = "employee_groups")
public class EmployeeGroup extends AuditableEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "is_default", nullable = false)
    private boolean defaultGroup;

    protected EmployeeGroup() {
        // für JPA
    }

    EmployeeGroup(Company company, String name, BigDecimal hourlyRate) {
        this.id = UUID.randomUUID();
        this.company = company;
        rename(name);
        changeHourlyRate(hourlyRate);
    }

    void rename(String name) {
        this.name = DomainRules.requireText(name, "Name der Lohngruppe", 100);
    }

    public void changeHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = DomainRules.requireNonNegative(hourlyRate, "Stundensatz").setScale(2, RoundingMode.HALF_UP);
    }

    void setDefaultGroup(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public boolean isDefaultGroup() {
        return defaultGroup;
    }
}
