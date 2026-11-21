package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.UUID;

/** Bankverbindung eines Betriebs, die auf Rechnungen erscheint. Wird über {@link Company} verwaltet. */
@Entity
@Table(name = "bank_accounts")
public class BankAccount extends AuditableEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;

    @Column(name = "account_holder", nullable = false, length = 200)
    private String accountHolder;

    @Column(name = "iban", nullable = false, length = 34)
    private String iban;

    @Column(name = "bic", length = 11)
    private String bic;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAccount;

    protected BankAccount() {
        // für JPA
    }

    BankAccount(Company company, String accountHolder, String iban, String bic, String bankName) {
        this.id = UUID.randomUUID();
        this.company = company;
        update(accountHolder, iban, bic, bankName);
    }

    /** Ändert die Kontodaten. Die IBAN wird normalisiert und auf ihre Prüfsumme geprüft. */
    public void update(String accountHolder, String iban, String bic, String bankName) {
        this.accountHolder = DomainRules.requireText(accountHolder, "Kontoinhaber", 200);
        this.iban = Iban.requireValid(iban);
        String normalizedBic = DomainRules.optionalText(bic, "BIC", 11);
        if (normalizedBic != null && !normalizedBic.toUpperCase(Locale.ROOT).matches("^[A-Z0-9]{8}([A-Z0-9]{3})?$")) {
            throw new IllegalArgumentException("BIC ist ungültig");
        }
        this.bic = normalizedBic == null ? null : normalizedBic.toUpperCase(Locale.ROOT);
        this.bankName = DomainRules.optionalText(bankName, "Bankname", 200);
    }

    void setDefaultAccount(boolean defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public String getIban() {
        return iban;
    }

    public String getMaskedIban() {
        return Iban.mask(iban);
    }

    public String getBic() {
        return bic;
    }

    public String getBankName() {
        return bankName;
    }

    public boolean isDefaultAccount() {
        return defaultAccount;
    }
}
