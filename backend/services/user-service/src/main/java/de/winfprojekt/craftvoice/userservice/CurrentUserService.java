package de.winfprojekt.craftvoice.userservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Locale;

@ApplicationScoped
public class CurrentUserService {

    private final JsonWebToken jwt;
    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    @Inject
    public CurrentUserService(
            JsonWebToken jwt,
            UserRepository userRepository,
            IdentityProvider identityProvider
    ) {
        this.jwt = jwt;
        this.userRepository = userRepository;
        this.identityProvider = identityProvider;
    }

    @Transactional
    public UserEntity syncUserWithDatabase() {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaim("email");

        if (keycloakId == null || keycloakId.isBlank()) {
            throw new BadRequestException("User hat kein subject im JWT-Token");
        }
        if (email == null || email.isBlank()) {
            throw new BadRequestException("User hat kein email-Claim im JWT-Token (Keycloak-Konfiguration überprüfen)");
        }

        UserEntity user = userRepository.findByKeycloakId(keycloakId);

        if (user == null) {
            throw new NotFoundException("User is not registered");
        }

        boolean activatingPendingUser = user.status == UserStatus.PENDING;
        if (activatingPendingUser) {
            Boolean emailVerified = jwt.getClaim("email_verified");
            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new ForbiddenException("Email address has not been verified");
            }

            if (!normalizeEmail(user.email).equals(normalizeEmail(email))) {
                throw new ForbiddenException("Verified email does not match the registered address");
            }

            identityProvider.assignRealmRole(keycloakId, UserRole.OWNER.name());
            user.roles.clear();
            user.roles.add(UserRole.OWNER);
            user.status = UserStatus.ACTIVE;
        } else if (user.status != UserStatus.ACTIVE) {
            throw new ForbiddenException("User account is not active");
        }

        if (!activatingPendingUser) {
            user.email = email;
        }

        if (user.firstName == null || user.firstName.isBlank()) {
            user.firstName = jwt.getClaim("given_name");
        }

        if (user.lastName == null || user.lastName.isBlank()) {
            user.lastName = jwt.getClaim("family_name");
        }

        return user;
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
