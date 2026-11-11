package de.winfprojekt.craftvoice.userservice.infrastructure;

import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;

@ApplicationScoped
public class KeycloakUserAdapter implements IdentityProvider {

    private final Keycloak keycloak;
    private final String realm;

    @Inject
    public KeycloakUserAdapter(
            Keycloak keycloak,
            @ConfigProperty(name = "userservice.keycloak.realm", defaultValue = "handwerker-realm") String realm
    ) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    public String createUser(String email, String firstName, String lastName, String password) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setRequiredActions(Collections.singletonList("VERIFY_EMAIL"));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() != 201) {
                throw new BadRequestException(
                        "Could not create user in Keycloak: " + response.readEntity(String.class)
                );
            }

            return response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        }
    }

    @Override
    public void sendVerificationEmail(String userId) {
        keycloak.realm(realm).users().get(userId).sendVerifyEmail();
    }

    @Override
    public void assignRealmRole(String userId, String roleName) {
        var realmResource = keycloak.realm(realm);
        var role = realmResource.roles().get(roleName).toRepresentation();
        if (role == null) {
            throw new NotFoundException("Required Keycloak role is not configured");
        }

        var roleMappings = realmResource.users().get(userId).roles().realmLevel();
        roleMappings.add(Collections.singletonList(role));

        boolean assigned = roleMappings.listAll().stream()
                .anyMatch(assignedRole -> roleName.equals(assignedRole.getName()));
        if (!assigned) {
            throw new WebApplicationException("Required Keycloak role assignment was not confirmed");
        }
    }

    @Override
    public void sendPasswordResetEmail(String email) {
        var users = keycloak.realm(realm).users().search(email, true);
        if (!users.isEmpty()) {
            keycloak.realm(realm).users().get(users.get(0).getId())
                    .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
        }
    }

    @Override
    public void deleteUser(String userId) {
        keycloak.realm(realm).users().get(userId).remove();
    }

    @Override
    public void updateName(String userId, String firstName, String lastName) {
        var userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        userResource.update(user);
    }
}
