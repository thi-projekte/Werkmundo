/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS:
 *   backend/services/user-service/src/main/java/de/winfprojekt/craftvoice/userservice/UserEntity.java
 * NEUE SCHICHT: domain  (ersetzt den leeren Platzhalter domain/User.java)
 * -------------------------------------------------------------------------------------
 * WAS JETZT IN DIESER DATEI ZU TUN IST:
 *
 * 1. Package anpassen (bewusst noch NICHT geändert):
 *      de.winfprojekt.craftvoice.userservice -> de.winfprojekt.craftvoice.userservice.domain
 *
 * 2. Domain vs. Persistenz trennen: Die Klasse ist gleichzeitig Domain-Objekt UND
 *    JPA/Panache-Entity (Active-Record-Pattern). Mindestens:
 *    - findByEmail() / findByKeycloakId() (unten) in domain/UserRepository.java
 *      (Interface) verschieben und in infrastructure/UserJpaRepository.java
 *      (implements PanacheRepository<UserEntity>) implementieren.
 *    Optional (sauberer): reines Domain-Objekt domain/User.java ohne JPA-Annotationen
 *    + JPA-Entity in infrastructure/ + Mapper dazwischen.
 *
 * 3. Klasse ist zu breit ("God-Entity"): Person + Firma + Adresse + Bank + Steuer +
 *    Anfahrtskosten + KI-Stil + Textbausteine + Kunden-Besitzer (ownerId) in EINER Tabelle.
 *    Vorschlag:
 *    - domain/Company.java (oder @Embeddable CompanyProfile): companyName ... priceListUrl,
 *      Bank-, Steuer-, Anfahrts- und KI-Stil-Felder, agbNotes, paymentTerms.
 *    - domain/Address.java als @Embeddable (street, houseNumber, zipCode, city, state, country).
 *    - ownerId + Rolle CUSTOMER = Kunden -> gehört zu Squad J (siehe UserService.java).
 *
 * 4. Die Enums UserStatus und UserRole (ganz unten, package-private) in eigene Dateien
 *    domain/UserStatus.java und domain/UserRole.java verschieben und public machen
 *    (werden sonst nach dem Package-Umzug aus application/ nicht mehr gefunden).
 *
 * 5. Abgleich mit dem Frontend-Typ Beispiel/frontend/Model/user/UserProfile.ts:
 *    Frontend kennt "termsOfPayment"/"disclaimer", Backend heißt es "paymentTerms"/"agbNotes";
 *    travelModel/travelFlatRate/travelKmRate fehlen im Frontend-Typ.
 *    -> Namen vereinheitlichen (am besten über die API-DTOs, siehe api/UserResource.java).
 * =====================================================================================
 */
package de.winfprojekt.craftvoice.userservice;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @NotBlank
    @Email
    @Column(unique = true)
    public String email;

    @Column(unique = true)
    public String keycloakId;

    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    public UserStatus status = UserStatus.PENDING;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    public Set<UserRole> roles = new HashSet<>();

    // Company Metadata
    public String companyName;
    public String vatId;
    public String tradeRegisterNumber;

    // Detailed Address
    public String street;
    public String houseNumber;
    public String zipCode;
    public String city;
    public String state;
    public String country;

    // Company Contact
    public String companyEmail;
    public String companyPhoneNumber;
    public String website;
    public String industry;

    // Banking Info
    public String iban;
    public String bic;
    public String bankName;
    public String accountHolder;

    // Tax Info
    public String taxNumber;
    public String legalForm;

    // Business Details
    public Integer employeeCount;
    public Integer customerCount;
    public Double hourlyRate;
    public String priceListUrl;

    // Travel / Anfahrtskosten configuration
    // Allowed values: "PAUSCHALE", "PAUSCHALE_PLUS_KM", "NUR_KM"
    public String travelModel;

    @Column(precision = 19, scale = 4)
    public BigDecimal travelFlatRate;

    @Column(precision = 19, scale = 4)
    public BigDecimal travelKmRate;

    // AI Style Preferences
    public String toneOfVoice;
    public String detailLevel;

    // Text Blocks
    @Column(columnDefinition = "TEXT")
    public String agbNotes;

    @Column(columnDefinition = "TEXT")
    public String paymentTerms;

    // Owner tracking for customers (null for OWNER/EMPLOYEE roles)
    // Stores the ID of the craftsman who created this customer
    public Long ownerId;

    // TODO: Finder -> domain/UserRepository.java + infrastructure/UserJpaRepository.java
    public static UserEntity findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public static UserEntity findByKeycloakId(String keycloakId) {
        return find("keycloakId", keycloakId).firstResult();
    }
}

// TODO: -> eigene Datei domain/UserStatus.java (public)
enum UserStatus {
    PENDING,
    ACTIVE,
    DELETED
}

// TODO: -> eigene Datei domain/UserRole.java (public)
enum UserRole {
    OWNER,
    EMPLOYEE,
    ACCOUNTANT,
    CUSTOMER
}