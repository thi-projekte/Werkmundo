package de.winfprojekt.werkmundo.userservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Handwerksbetrieb und Mittelpunkt der user-db.
 *
 * <p>Bankkonten, Lohngruppen, Texte und die 1:1-Einstellungen werden ausschließlich über diese Klasse
 * geändert, damit Regeln wie "genau ein Standardkonto" an einer Stelle gelten.
 */
@Entity
@Table(name = "companies")
public class Company extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "legal_form", length = 50)
    private String legalForm;

    @Column(name = "trade", length = 100)
    private String trade;

    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "house_number", length = 20)
    private String houseNumber;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", nullable = false, length = 2)
    private String country = "DE";

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "contact_person", length = 200)
    private String contactPerson;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "trade_register_number", length = 50)
    private String tradeRegisterNumber;

    @Column(name = "register_court", length = 100)
    private String registerCourt;

    @Column(name = "chamber_registration", length = 100)
    private String chamberRegistration;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<BankAccount> bankAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<EmployeeGroup> employeeGroups = new ArrayList<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TextTemplate> textTemplates = new ArrayList<>();

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TaxInvoiceSettings taxInvoiceSettings;

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TravelCostSettings travelCostSettings;

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AiPreferences aiPreferences;

    protected Company() {
        // für JPA
    }

    /** Legt einen Betrieb an, wie bei der Registrierung: nur der Name ist Pflicht. */
    public Company(String name) {
        this.id = UUID.randomUUID();
        rename(name);
        this.aiPreferences = new AiPreferences(this);
    }

    // ---------------------------------------------------------------- Stammdaten

    public void rename(String name) {
        this.name = DomainRules.requireText(name, "Firmenname", 200);
    }

    public void changeLegalDetails(String legalForm, String trade, String tradeRegisterNumber,
            String registerCourt, String chamberRegistration) {
        this.legalForm = DomainRules.optionalText(legalForm, "Rechtsform", 50);
        this.trade = DomainRules.optionalText(trade, "Gewerk", 100);
        this.tradeRegisterNumber = DomainRules.optionalText(tradeRegisterNumber, "Handelsregisternummer", 50);
        this.registerCourt = DomainRules.optionalText(registerCourt, "Registergericht", 100);
        this.chamberRegistration = DomainRules.optionalText(chamberRegistration, "Handwerksrolle", 100);
    }

    public void changeAddress(String street, String houseNumber, String postalCode, String city, String country) {
        this.street = DomainRules.optionalText(street, "Straße", 200);
        this.houseNumber = DomainRules.optionalText(houseNumber, "Hausnummer", 20);
        this.postalCode = DomainRules.optionalText(postalCode, "PLZ", 10);
        this.city = DomainRules.optionalText(city, "Ort", 100);
        String countryCode = DomainRules.optionalText(country, "Land", 2);
        if (countryCode != null && !countryCode.matches("^[A-Za-z]{2}$")) {
            throw new IllegalArgumentException("Land muss ein ISO-Ländercode sein, z. B. DE");
        }
        this.country = countryCode == null ? "DE" : countryCode.toUpperCase(Locale.ROOT);
    }

    public void changeContact(String email, String phone, String contactPerson, String website) {
        this.email = DomainRules.optionalEmail(email, "E-Mail des Betriebs");
        this.phone = DomainRules.optionalText(phone, "Telefon", 40);
        this.contactPerson = DomainRules.optionalText(contactPerson, "Ansprechpartner", 200);
        this.website = DomainRules.optionalText(website, "Website", 300);
    }

    public void changeLogoUrl(String logoUrl) {
        this.logoUrl = DomainRules.optionalText(logoUrl, "Logo-URL", 500);
    }

    /**
     * Prüft, ob alle Angaben vorhanden sind, die auf einer Rechnung stehen müssen.
     * Erst dann darf das Onboarding abgeschlossen werden.
     */
    public List<String> missingInvoiceRequirements() {
        List<String> missing = new ArrayList<>();
        if (legalForm == null) {
            missing.add("Rechtsform");
        }
        if (trade == null) {
            missing.add("Gewerk");
        }
        if (street == null || houseNumber == null || postalCode == null || city == null) {
            missing.add("Adresse");
        }
        if (email == null || phone == null) {
            missing.add("Kontakt");
        }
        if (taxInvoiceSettings == null) {
            missing.add("Steuer- und Rechnungseinstellungen");
        }
        if (defaultBankAccount().isEmpty()) {
            missing.add("Bankverbindung");
        }
        if (defaultEmployeeGroup().isEmpty()) {
            missing.add("Lohngruppe mit Stundensatz");
        }
        return missing;
    }

    // ---------------------------------------------------------------- Bankkonten

    /** Fügt ein Konto hinzu. Das erste Konto wird automatisch Standardkonto. */
    public BankAccount addBankAccount(String accountHolder, String iban, String bic, String bankName) {
        String normalized = Iban.normalize(iban);
        if (bankAccounts.stream().anyMatch(account -> account.getIban().equals(normalized))) {
            throw new IllegalArgumentException("Diese IBAN ist bereits hinterlegt");
        }
        BankAccount account = new BankAccount(this, accountHolder, iban, bic, bankName);
        account.setDefaultAccount(bankAccounts.isEmpty());
        bankAccounts.add(account);
        return account;
    }

    public void markDefaultBankAccount(UUID bankAccountId) {
        BankAccount target = findBankAccount(bankAccountId);
        bankAccounts.forEach(account -> account.setDefaultAccount(account == target));
    }

    /** Entfernt ein Konto. War es das Standardkonto, übernimmt das älteste verbleibende Konto. */
    public void removeBankAccount(UUID bankAccountId) {
        BankAccount target = findBankAccount(bankAccountId);
        bankAccounts.remove(target);
        if (target.isDefaultAccount() && !bankAccounts.isEmpty()) {
            bankAccounts.get(0).setDefaultAccount(true);
        }
    }

    public Optional<BankAccount> defaultBankAccount() {
        return bankAccounts.stream().filter(BankAccount::isDefaultAccount).findFirst();
    }

    public BankAccount findBankAccount(UUID bankAccountId) {
        return bankAccounts.stream()
                .filter(account -> account.getId().equals(bankAccountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bankkonto gehört nicht zu diesem Betrieb"));
    }

    // ---------------------------------------------------------------- Lohngruppen

    /** Fügt eine Lohngruppe hinzu. Die erste Gruppe wird automatisch Standard. */
    public EmployeeGroup addEmployeeGroup(String name, BigDecimal hourlyRate) {
        EmployeeGroup group = new EmployeeGroup(this, name, hourlyRate);
        ensureUniqueGroupName(group.getName(), null);
        group.setDefaultGroup(employeeGroups.isEmpty());
        employeeGroups.add(group);
        return group;
    }

    public void renameEmployeeGroup(UUID groupId, String newName) {
        EmployeeGroup group = findEmployeeGroup(groupId);
        String trimmed = DomainRules.requireText(newName, "Name der Lohngruppe", 100);
        ensureUniqueGroupName(trimmed, group);
        group.rename(trimmed);
    }

    public void markDefaultEmployeeGroup(UUID groupId) {
        EmployeeGroup target = findEmployeeGroup(groupId);
        employeeGroups.forEach(group -> group.setDefaultGroup(group == target));
    }

    public void removeEmployeeGroup(UUID groupId) {
        EmployeeGroup target = findEmployeeGroup(groupId);
        employeeGroups.remove(target);
        if (target.isDefaultGroup() && !employeeGroups.isEmpty()) {
            employeeGroups.get(0).setDefaultGroup(true);
        }
    }

    public Optional<EmployeeGroup> defaultEmployeeGroup() {
        return employeeGroups.stream().filter(EmployeeGroup::isDefaultGroup).findFirst();
    }

    public EmployeeGroup findEmployeeGroup(UUID groupId) {
        return employeeGroups.stream()
                .filter(group -> group.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Lohngruppe gehört nicht zu diesem Betrieb"));
    }

    private void ensureUniqueGroupName(String candidate, EmployeeGroup except) {
        boolean taken = employeeGroups.stream()
                .filter(group -> group != except)
                .anyMatch(group -> group.getName().equalsIgnoreCase(candidate));
        if (taken) {
            throw new IllegalArgumentException("Lohngruppe \"" + candidate + "\" gibt es bereits");
        }
    }

    // ---------------------------------------------------------------- Steuer, Anfahrt, KI

    /** Legt die Steuer- und Rechnungseinstellungen an oder ändert sie. */
    public TaxInvoiceSettings configureTaxInvoice(TaxInvoiceData data) {
        if (taxInvoiceSettings == null) {
            taxInvoiceSettings = new TaxInvoiceSettings(this, data);
        } else {
            taxInvoiceSettings.apply(data);
        }
        return taxInvoiceSettings;
    }

    /** Legt die Anfahrtskosten an oder ändert sie. */
    public TravelCostSettings configureTravelCosts(TravelCostData data) {
        if (travelCostSettings == null) {
            travelCostSettings = new TravelCostSettings(this, data);
        } else {
            travelCostSettings.apply(data);
        }
        return travelCostSettings;
    }

    public void removeTravelCosts() {
        travelCostSettings = null;
    }

    // ---------------------------------------------------------------- Texte

    /** Setzt den Text eines Typs; vorhandener Text wird überschrieben. */
    public TextTemplate putTextTemplate(TextTemplateType type, String content) {
        Optional<TextTemplate> existing = findTextTemplate(type);
        if (existing.isPresent()) {
            existing.get().changeContent(content);
            return existing.get();
        }
        TextTemplate template = new TextTemplate(this, type, content);
        textTemplates.add(template);
        return template;
    }

    public void removeTextTemplate(TextTemplateType type) {
        textTemplates.removeIf(template -> template.getType() == type);
    }

    public Optional<TextTemplate> findTextTemplate(TextTemplateType type) {
        return textTemplates.stream().filter(template -> template.getType() == type).findFirst();
    }

    // ---------------------------------------------------------------- Getter

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLegalForm() {
        return legalForm;
    }

    public String getTrade() {
        return trade;
    }

    public String getStreet() {
        return street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getWebsite() {
        return website;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getTradeRegisterNumber() {
        return tradeRegisterNumber;
    }

    public String getRegisterCourt() {
        return registerCourt;
    }

    public String getChamberRegistration() {
        return chamberRegistration;
    }

    public List<BankAccount> getBankAccounts() {
        return Collections.unmodifiableList(bankAccounts);
    }

    public List<EmployeeGroup> getEmployeeGroups() {
        return Collections.unmodifiableList(employeeGroups);
    }

    public List<TextTemplate> getTextTemplates() {
        return Collections.unmodifiableList(textTemplates);
    }

    public Optional<TaxInvoiceSettings> getTaxInvoiceSettings() {
        return Optional.ofNullable(taxInvoiceSettings);
    }

    public Optional<TravelCostSettings> getTravelCostSettings() {
        return Optional.ofNullable(travelCostSettings);
    }

    public AiPreferences getAiPreferences() {
        return aiPreferences;
    }
}
