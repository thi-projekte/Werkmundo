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
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Steuer- und Rechnungseinstellungen eines Betriebs (1:1). Wird über {@link Company} verwaltet. */
@Entity
@Table(name = "tax_invoice_settings")
public class TaxInvoiceSettings extends AuditableEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "vat_treatment", nullable = false, length = 20)
    private VatTreatment vatTreatment;

    @Column(name = "default_vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultVatRate;

    @Column(name = "tax_number", length = 30)
    private String taxNumber;

    @Column(name = "vat_id", length = 20)
    private String vatId;

    @Column(name = "invoice_prefix", nullable = false, length = 20)
    private String invoicePrefix;

    @Column(name = "offer_prefix", length = 20)
    private String offerPrefix;

    @Column(name = "payment_term_days", nullable = false)
    private int paymentTermDays;

    @Column(name = "invoice_email", length = 254)
    private String invoiceEmail;

    protected TaxInvoiceSettings() {
        // für JPA
    }

    TaxInvoiceSettings(Company company, TaxInvoiceData data) {
        this.company = company;
        apply(data);
    }

    void apply(TaxInvoiceData data) {
        DomainRules.requireValue(data, "Steuer- und Rechnungsdaten");
        VatTreatment treatment = DomainRules.requireValue(data.vatTreatment(), "Umsatzsteuer-Behandlung");
        BigDecimal rate = DomainRules.requireNonNegative(data.defaultVatRate(), "Steuersatz").setScale(2, RoundingMode.HALF_UP);
        if (rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Steuersatz darf höchstens 100 % sein");
        }
        if (treatment == VatTreatment.SMALL_BUSINESS && rate.signum() != 0) {
            throw new IllegalArgumentException("Kleinunternehmer (§ 19 UStG) weisen keine Umsatzsteuer aus; Steuersatz muss 0 sein");
        }
        String newTaxNumber = DomainRules.optionalText(data.taxNumber(), "Steuernummer", 30);
        String newVatId = DomainRules.optionalText(data.vatId(), "USt-IdNr.", 20);
        if (newTaxNumber == null && newVatId == null) {
            throw new IllegalArgumentException("Steuernummer oder USt-IdNr. muss angegeben werden");
        }
        if (data.paymentTermDays() < 0) {
            throw new IllegalArgumentException("Zahlungsziel darf nicht negativ sein");
        }

        this.vatTreatment = treatment;
        this.defaultVatRate = rate;
        this.taxNumber = newTaxNumber;
        this.vatId = newVatId == null ? null : newVatId.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        this.invoicePrefix = DomainRules.requireText(data.invoicePrefix(), "Rechnungspräfix", 20);
        this.offerPrefix = DomainRules.optionalText(data.offerPrefix(), "Angebotspräfix", 20);
        this.paymentTermDays = data.paymentTermDays();
        this.invoiceEmail = DomainRules.optionalEmail(data.invoiceEmail(), "Rechnungs-E-Mail");
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public Company getCompany() {
        return company;
    }

    public VatTreatment getVatTreatment() {
        return vatTreatment;
    }

    public BigDecimal getDefaultVatRate() {
        return defaultVatRate;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public String getVatId() {
        return vatId;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public String getOfferPrefix() {
        return offerPrefix;
    }

    public int getPaymentTermDays() {
        return paymentTermDays;
    }

    public String getInvoiceEmail() {
        return invoiceEmail;
    }
}
