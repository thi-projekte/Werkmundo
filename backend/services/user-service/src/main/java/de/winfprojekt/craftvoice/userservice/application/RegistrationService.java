package de.winfprojekt.craftvoice.userservice.application;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.UserRole;
import de.winfprojekt.craftvoice.userservice.UserStatus;
import de.winfprojekt.craftvoice.userservice.application.port.AuditLogger;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class RegistrationService {

    private final IdentityProvider identityProvider;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Inject
    public RegistrationService(
            IdentityProvider identityProvider,
            UserRepository userRepository,
            AuditLogger auditLogger
    ) {
        this.identityProvider = identityProvider;
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public void register(UserEntity userData, String password) {
        if (userRepository.findByEmail(userData.email) != null) {
            throw new BadRequestException("Email already registered");
        }

        String keycloakId = identityProvider.createUser(
                userData.email,
                userData.firstName,
                userData.lastName,
                password
        );

        userData.keycloakId = keycloakId;
        userData.status = UserStatus.PENDING;
        userData.roles.clear();
        // Keycloak and PostgreSQL do not share a transaction; a database failure after
        // role assignment can leave the Keycloak role ahead of the local activation state.
        userRepository.persistUser(userData);

        identityProvider.sendVerificationEmail(keycloakId);

        auditLogger.log(userData.id, "REGISTRATION", "User registered via Keycloak");
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        identityProvider.sendPasswordResetEmail(email);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        UserEntity user = userRepository.findById(userId);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (user.keycloakId != null && !user.keycloakId.isBlank()) {
            identityProvider.deleteUser(user.keycloakId);
        }

        // Keycloak deletion precedes local anonymization; these systems cannot be committed atomically.
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

        auditLogger.log(
                userId,
                "ACCOUNT_DELETED",
                "User account deleted and anonymized locally"
        );
    }
}
