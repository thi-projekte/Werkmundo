package de.winfprojekt.craftvoice.userservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
            user = provisionFromToken(keycloakId, email);
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
            replaceAppRoles(user);
        }

        if (user.firstName == null || user.firstName.isBlank()) {
            user.firstName = jwt.getClaim("given_name");
        }

        if (user.lastName == null || user.lastName.isBlank()) {
            user.lastName = jwt.getClaim("family_name");
        }

        return user;
    }

    private UserEntity provisionFromToken(String keycloakId, String email) {
        UserEntity existing = userRepository.findByNormalizedEmail(normalizeEmail(email));
        if (existing != null) {
            if (keycloakId.equals(existing.keycloakId)) {
                return existing;
            }
            throw new ClientErrorException("Email already registered", 409);
        }

        UserEntity user = new UserEntity();
        user.keycloakId = keycloakId;
        user.email = email;
        user.status = UserStatus.ACTIVE;
        applyNameIfBlank(user);
        replaceAppRoles(user);
        userRepository.persistUser(user);
        return user;
    }

    private void replaceAppRoles(UserEntity user) {
        user.roles.clear();
        user.roles.addAll(rolesFromToken());
    }

    private void applyNameIfBlank(UserEntity user) {
        if (user.firstName == null || user.firstName.isBlank()) {
            user.firstName = blankToNull(jwt.getClaim("given_name"));
        }
        if (user.lastName == null || user.lastName.isBlank()) {
            user.lastName = blankToNull(jwt.getClaim("family_name"));
        }
    }

    private Set<UserRole> rolesFromToken() {
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        for (String name : roleNames(jwt.getClaim("realm_access"))) {
            UserRole role = parseRole(name);
            if (role != null && role != UserRole.CUSTOMER) {
                roles.add(role);
            }
        }
        return roles;
    }

    private static UserRole parseRole(String name) {
        try {
            return UserRole.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static List<String> roleNames(Object realmAccess) {
        if (realmAccess instanceof JsonObject json) {
            JsonArray roles = json.getJsonArray("roles");
            if (roles == null) {
                return List.of();
            }
            List<String> names = new ArrayList<>();
            for (JsonValue value : roles) {
                if (value.getValueType() == JsonValue.ValueType.STRING) {
                    names.add(((JsonString) value).getString());
                }
            }
            return names;
        }
        if (realmAccess instanceof Map<?, ?> map) {
            Object roles = map.get("roles");
            if (roles instanceof Collection<?> collection) {
                List<String> names = new ArrayList<>();
                for (Object role : collection) {
                    if (role != null) {
                        names.add(role.toString());
                    }
                }
                return names;
            }
        }
        return List.of();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
