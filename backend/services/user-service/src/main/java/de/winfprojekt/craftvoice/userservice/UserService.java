package de.winfprojekt.craftvoice.userservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.ForbiddenException;
import java.util.EnumSet;
import java.util.Set;

@ApplicationScoped
public class UserService {

    @Inject
    Keycloak keycloak;

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity securityIdentity;

    @ConfigProperty(name = "DOCUMENT_STORAGE_PATH", defaultValue = "/tmp")
    String storagePath;

    @ConfigProperty(
    name = "app.registration.send-verification-email",
    defaultValue = "true"
    )
    boolean sendVerificationEmail;

    private static final String REALM = "handwerker-realm";

    @Transactional
    public String uploadProfilePicture(Long userId, FileUpload file) {
        UserEntity user = UserEntity.findById(userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        try {
            String fileName =
                    "profile_" + userId + "_" + System.currentTimeMillis() + getExtension(file.fileName());

            Path path = Paths.get(storagePath, "profiles", fileName);
            Files.createDirectories(path.getParent());
            Files.copy(file.uploadedFile(), path);

            String url = "/api/users/profile-picture/" + fileName;
            user.profilePictureUrl = url;

            AuditLogEntity.log(
                    user.id,
                    "PROFILE_PICTURE_UPLOAD",
                    "User uploaded a new profile picture: " + fileName
            );

            return url;
        } catch (IOException e) {
            throw new RuntimeException("Could not save profile picture", e);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(fileName.lastIndexOf("."));
    }

    
@Transactional
public void register(UserEntity userData, String password) {

    if (userData == null || userData.email == null
            || userData.email.isBlank()
            || password == null || password.isBlank()) {
        throw new BadRequestException(
                "Email and password are required"
        );
    }

    if (UserEntity.findByEmail(userData.email) != null) {
        throw new BadRequestException("Email already registered");
    }

    UserRepresentation user = new UserRepresentation();
    user.setUsername(userData.email);
    user.setEmail(userData.email);
    user.setFirstName(userData.firstName);
    user.setLastName(userData.lastName);
    user.setEnabled(true);
    user.setEmailVerified(false);

    CredentialRepresentation cred = new CredentialRepresentation();
    cred.setType(CredentialRepresentation.PASSWORD);
    cred.setValue(password);
    cred.setTemporary(false);
    user.setCredentials(Collections.singletonList(cred));

    String keycloakId;

    try (Response response =
            keycloak.realm(REALM).users().create(user)) {

        if (response.getStatus() != 201) {
            throw new BadRequestException(
                    "Could not create user in Keycloak: "
                    + response.getStatus()
            );
        }

        keycloakId = response.getLocation()
                .getPath()
                .replaceAll(".*/([^/]+)$", "$1");
    }

    try {
        // OWNER-Rolle muss in Keycloak vorhanden sein
        org.keycloak.representations.idm.RoleRepresentation ownerRole =
                keycloak.realm(REALM)
                        .roles()
                        .get("OWNER")
                        .toRepresentation();

        if (ownerRole == null) {
            throw new IllegalStateException(
                    "OWNER role not found in Keycloak"
            );
        }

        // OWNER-Rolle zuerst in Keycloak zuweisen
        keycloak.realm(REALM)
                .users()
                .get(keycloakId)
                .roles()
                .realmLevel()
                .add(Collections.singletonList(ownerRole));

        // Danach lokalen Benutzer speichern
        userData.keycloakId = keycloakId;
        userData.status = UserStatus.PENDING;
        userData.roles.add(UserRole.OWNER);
        userData.persistAndFlush();

       
        // E-Mail-Verifizierung außerhalb der lokalen Entwicklung anfordern
        if (sendVerificationEmail) {
            keycloak.realm(REALM)
                .users()
                .get(keycloakId)
                .sendVerifyEmail();
}

        AuditLogEntity.log(
                userData.id,
                "REGISTRATION",
                "OWNER registered via Keycloak"
        );

    } catch (RuntimeException e) {

        // Bei Fehler angelegten Keycloak-Benutzer bereinigen
        try {
            keycloak.realm(REALM)
                    .users()
                    .get(keycloakId)
                    .remove();
        } catch (Exception cleanupError) {
            e.addSuppressed(cleanupError);
        }

        throw e;
    }
}

@Transactional
public UserEntity syncUserWithDatabase() {

    String keycloakId = jwt.getSubject();
    String email = jwt.getClaim("email");

    if (keycloakId == null || keycloakId.isBlank()) {
        throw new ForbiddenException(
            "Invalid authenticated user"
        );
    }

    if (email == null || email.isBlank()) {
        throw new BadRequestException(
            "User has no email claim in JWT"
        );
    }

    // Rollen aus der authentifizierten Identität übernehmen
    Set<UserRole> assignedRoles =
        EnumSet.noneOf(UserRole.class);

    for (UserRole role : UserRole.values()) {
        if (securityIdentity.hasRole(role.name())) {
            assignedRoles.add(role);
        }
    }

    // Benutzer ohne gültige Anwendungsrolle ablehnen
    if (assignedRoles.isEmpty()) {
        throw new ForbiddenException(
            "User has no valid application role"
        );
    }

    UserEntity user =
        UserEntity.findByKeycloakId(keycloakId);

    if (user == null) {
        user = new UserEntity();

        user.keycloakId = keycloakId;
        user.email = email;
        user.firstName = jwt.getClaim("given_name");
        user.lastName = jwt.getClaim("family_name");
        user.status = UserStatus.ACTIVE;

        user.roles.addAll(assignedRoles);

        user.persist();
    } else {
        // Gelöschte Benutzer nicht reaktivieren
        if (user.status == UserStatus.DELETED) {
            throw new ForbiddenException(
                "User account is deleted"
            );
        }

        user.email = email;

        if (user.firstName == null ||
            user.firstName.isBlank()) {

            user.firstName = jwt.getClaim("given_name");
        }

        if (user.lastName == null ||
            user.lastName.isBlank()) {

            user.lastName = jwt.getClaim("family_name");
        }

        // Datenbankrollen mit Keycloak synchronisieren
        user.roles.clear();
        user.roles.addAll(assignedRoles);
    }

    return user;
}


    @Transactional
    public UserEntity updateProfile(Long userId, UserEntity data) {
        UserEntity user = UserEntity.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (data.firstName != null) {
            user.firstName = data.firstName;
        }

        if (data.lastName != null) {
            user.lastName = data.lastName;
        }

        if (data.phoneNumber != null) {
            user.phoneNumber = data.phoneNumber;
        }

        if (data.profilePictureUrl != null) {
            user.profilePictureUrl = data.profilePictureUrl;
        }

        try {
            if (user.keycloakId != null && !user.keycloakId.isBlank()) {
                UserRepresentation kcUser =
                        keycloak.realm(REALM).users().get(user.keycloakId).toRepresentation();

                if (data.firstName != null) {
                    kcUser.setFirstName(data.firstName);
                }

                if (data.lastName != null) {
                    kcUser.setLastName(data.lastName);
                }

                keycloak.realm(REALM).users().get(user.keycloakId).update(kcUser);
            }
        } catch (Exception e) {
            System.err.println(
                    "Keycloak profile sync failed for user "
                            + user.id
                            + ": "
                            + e.getMessage()
            );
        }

        AuditLogEntity.log(
                user.id,
                "PROFILE_UPDATE",
                "User updated personal data"
        );

        return user;
    }

    @Transactional
    public void updateCompanyData(Long userId, UserEntity data) {
        UserEntity user = UserEntity.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (data.companyName != null) user.companyName = data.companyName;
        if (data.vatId != null) user.vatId = data.vatId;
        if (data.tradeRegisterNumber != null) user.tradeRegisterNumber = data.tradeRegisterNumber;

        if (data.street != null) user.street = data.street;
        if (data.houseNumber != null) user.houseNumber = data.houseNumber;
        if (data.zipCode != null) user.zipCode = data.zipCode;
        if (data.city != null) user.city = data.city;
        if (data.state != null) user.state = data.state;
        if (data.country != null) user.country = data.country;

        if (data.companyEmail != null) user.companyEmail = data.companyEmail;
        if (data.companyPhoneNumber != null) user.companyPhoneNumber = data.companyPhoneNumber;
        if (data.website != null) user.website = data.website;
        if (data.industry != null) user.industry = data.industry;

        if (data.iban != null) user.iban = data.iban;
        if (data.bic != null) user.bic = data.bic;
        if (data.bankName != null) user.bankName = data.bankName;
        if (data.accountHolder != null) user.accountHolder = data.accountHolder;

        if (data.taxNumber != null) user.taxNumber = data.taxNumber;
        if (data.legalForm != null) user.legalForm = data.legalForm;

        if (data.employeeCount != null) user.employeeCount = data.employeeCount;
        if (data.customerCount != null) user.customerCount = data.customerCount;
        if (data.hourlyRate != null) user.hourlyRate = data.hourlyRate;
        if (data.priceListUrl != null) user.priceListUrl = data.priceListUrl;

        if (data.travelModel != null) user.travelModel = data.travelModel;
        if (data.travelFlatRate != null) user.travelFlatRate = data.travelFlatRate;
        if (data.travelKmRate != null) user.travelKmRate = data.travelKmRate;

        AuditLogEntity.log(
                user.id,
                "COMPANY_DATA_UPDATE",
                "User updated company metadata"
        );
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        List<UserRepresentation> users = keycloak.realm(REALM).users().search(email, true);

        if (users.isEmpty()) {
            return;
        }

        keycloak
                .realm(REALM)
                .users()
                .get(users.get(0).getId())
                .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
    }

    @Transactional
    public void deleteAccount(Long userId) {
        UserEntity user = UserEntity.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (user.keycloakId != null && !user.keycloakId.isBlank()) {
            keycloak.realm(REALM).users().get(user.keycloakId).remove();
        }

        user.status = UserStatus.DELETED;
        user.email = "deleted_" + user.id + "@handwerker.de";
        user.keycloakId = null;
        user.firstName = null;
        user.lastName = null;
        user.phoneNumber = null;
        user.profilePictureUrl = null;
        user.companyName = null;
        user.vatId = null;
        user.tradeRegisterNumber = null;

        user.street = null;
        user.houseNumber = null;
        user.zipCode = null;
        user.city = null;
        user.state = null;
        user.country = null;

        user.companyEmail = null;
        user.companyPhoneNumber = null;
        user.website = null;
        user.iban = null;
        user.bic = null;
        user.bankName = null;
        user.accountHolder = null;
        user.taxNumber = null;
        user.priceListUrl = null;

        AuditLogEntity.log(
                userId,
                "ACCOUNT_DELETED",
                "User account deleted and anonymized locally"
        );
    }

    @Transactional
    public UserEntity createCustomer(UserEntity customerData) {
        if (UserEntity.findByEmail(customerData.email) != null) {
            throw new BadRequestException("User with this email already exists");
        }

        // Track which craftsman created this customer
        UserEntity owner = syncUserWithDatabase();
        if (owner == null) {
            throw new BadRequestException("Could not determine current user");
        }

        customerData.status = UserStatus.ACTIVE;
        customerData.roles.add(UserRole.CUSTOMER);
        customerData.ownerId = owner.id;
        customerData.persist();

        AuditLogEntity.log(
                customerData.id,
                "CUSTOMER_CREATED",
                "Customer profile created by craftsman " + owner.id
        );

        return customerData;
    }

    @Transactional
    public UserEntity updateCustomer(Long id, UserEntity data) {
        UserEntity customer = UserEntity.findById(id);

        
        if (customer == null
            || !customer.roles.contains(UserRole.CUSTOMER)
            || customer.status == UserStatus.DELETED) {
            throw new NotFoundException("Customer not found");
        }


        // Verify ownership - only the craftsman who created the customer can update it
        UserEntity owner = syncUserWithDatabase();
        if (owner == null || !owner.id.equals(customer.ownerId)) {
            throw new NotFoundException("Customer not found");
        }

        // Allow updating common fields if provided
        if (data.firstName != null) customer.firstName = data.firstName;
        if (data.lastName != null) customer.lastName = data.lastName;
        if (data.phoneNumber != null) customer.phoneNumber = data.phoneNumber;
        if (data.email != null) {
            // Prevent email collision with existing users
            UserEntity existing = UserEntity.findByEmail(data.email);
            if (existing != null && !existing.id.equals(id)) {
                throw new BadRequestException("Email already in use");
            }
            customer.email = data.email;
        }

        // Allow updating address fields if provided
        if (data.street != null) customer.street = data.street;
        if (data.houseNumber != null) customer.houseNumber = data.houseNumber;
        if (data.zipCode != null) customer.zipCode = data.zipCode;
        if (data.city != null) customer.city = data.city;
        if (data.state != null) customer.state = data.state;
        if (data.country != null) customer.country = data.country;

        AuditLogEntity.log(
                customer.id,
                "CUSTOMER_UPDATED",
                "Customer data updated by craftsman " + owner.id
        );

        return customer;
    }

    
    @Transactional
    public void deleteCustomer(Long id) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null
                || !customer.roles.contains(UserRole.CUSTOMER)
                || customer.status == UserStatus.DELETED) {
            throw new NotFoundException("Customer not found");
        }

        UserEntity owner = syncUserWithDatabase();

        if (owner == null || !owner.id.equals(customer.ownerId)) {
            throw new NotFoundException("Customer not found");
        }

        customer.status = UserStatus.DELETED;
        customer.email = "deleted_customer_" + customer.id + "@handwerker.de";
        customer.keycloakId = null;
        customer.firstName = null;
        customer.lastName = null;
        customer.phoneNumber = null;
        customer.profilePictureUrl = null;

        AuditLogEntity.log(
            customer.id,
            "CUSTOMER_DELETED",
            "Customer deleted/anonymized by craftsman " + owner.id
        );
    }

    @Transactional
    public List<UserEntity> listCustomers() {
        UserEntity owner = syncUserWithDatabase();

        if (owner == null) {
            throw new BadRequestException(
                "Could not determine current user"
            );
        }

        return UserEntity.list(
            "select distinct u from UserEntity u " +
            "join u.roles r " +
            "where r = ?1 " +
            "and u.ownerId = ?2 " +
            "and u.status = ?3",
            UserRole.CUSTOMER,
            owner.id,
            UserStatus.ACTIVE
        );
    }

    @Transactional
    public UserEntity getCustomerById(Long id) {
        UserEntity customer = UserEntity.findById(id);

        if (customer == null
                || !customer.roles.contains(UserRole.CUSTOMER)
                || customer.status == UserStatus.DELETED) {
            throw new NotFoundException("Customer not found");
        }

        UserEntity owner = syncUserWithDatabase();

        if (owner == null || !owner.id.equals(customer.ownerId)) {
            throw new NotFoundException("Customer not found");
        }

        return customer;
    }



    
@Transactional
public void updateTravelConfig(Long userId, Map<String, Object> data) {
    UserEntity user = UserEntity.findById(userId);

    if (user == null) {
        throw new NotFoundException("User not found");
    }

    if (data == null) {
        throw new BadRequestException("Request data must not be null");
    }

    if (data.containsKey("modell")) {
        user.travelModel = optionalString(data.get("modell"), "modell");
    }

    if (data.containsKey("pauschale")) {
        user.travelFlatRate = optionalDecimal(data.get("pauschale"), "pauschale");
    }

    if (data.containsKey("kmSatz")) {
        user.travelKmRate = optionalDecimal(data.get("kmSatz"), "kmSatz");
    }

    if (data.containsKey("adresse")) {
        Object rawAddress = data.get("adresse");

        if (rawAddress != null) {
            if (!(rawAddress instanceof Map<?, ?> address)) {
                throw new BadRequestException(
                    "adresse must be an object"
                );
            }

            if (address.containsKey("strasse")) {
                user.street = optionalString(address.get("strasse"), "strasse");
            }
            if (address.containsKey("hausnummer")) {
                user.houseNumber = optionalString(address.get("hausnummer"), "hausnummer");
            }
            if (address.containsKey("plz")) {
                user.zipCode = optionalString(address.get("plz"), "plz");
            }
            if (address.containsKey("ort")) {
                user.city = optionalString(address.get("ort"), "ort");
            }
            if (address.containsKey("bundesland")) {
                user.state = optionalString(address.get("bundesland"), "bundesland");
            }
            if (address.containsKey("land")) {
                user.country = optionalString(address.get("land"), "land");
            }
        }
    }

    AuditLogEntity.log(
        user.id,
        "TRAVEL_CONFIG_UPDATE",
        "User updated travel configuration and address details"
    );
}

private String optionalString(Object value, String field) {
    if (value == null) {
        return null;
    }

    if (!(value instanceof String text)) {
        throw new BadRequestException(field + " must be a string");
    }

    return text;
}

private BigDecimal optionalDecimal(Object value, String field) {
    if (value == null) {
        return null;
    }

    try {
        BigDecimal result = new BigDecimal(value.toString());

        if (result.signum() < 0) {
            throw new BadRequestException(
                field + " must not be negative"
            );
        }

        return result;
    } catch (NumberFormatException e) {
        throw new BadRequestException(
            field + " must be a valid decimal number"
        );
    }
}
}
