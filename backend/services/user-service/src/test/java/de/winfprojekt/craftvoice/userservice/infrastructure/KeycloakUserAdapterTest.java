package de.winfprojekt.craftvoice.userservice.infrastructure;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class KeycloakUserAdapterTest {

    @Test
    void newKeycloakUsersMustCompleteEmailVerificationBeforeLogin() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        Response response = mock(Response.class);
        when(keycloak.realm("handwerker-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create(
                "/admin/realms/handwerker-realm/users/keycloak-user"
        ));

        KeycloakUserAdapter adapter = new KeycloakUserAdapter(keycloak, "handwerker-realm");
        assertEquals("keycloak-user", adapter.createUser("user@example.com", "Ada", "Lovelace", "password"));

        var userCaptor = org.mockito.ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(userCaptor.capture());
        assertFalse(userCaptor.getValue().isEmailVerified());
        assertEquals(List.of("VERIFY_EMAIL"), userCaptor.getValue().getRequiredActions());
        verify(response).close();
    }

    @Test
    void missingRealmRoleFailsInsteadOfReportingSuccess() {
        RoleSetup setup = roleSetup();
        when(setup.realmRoles.get("OWNER")).thenReturn(setup.roleResource);
        when(setup.roleResource.toRepresentation()).thenReturn(null);

        assertThrows(NotFoundException.class, () -> setup.adapter.assignRealmRole("user-id", "OWNER"));

        verify(setup.roleScope, never()).add(any());
        verify(setup.roleScope, never()).listAll();
    }

    @Test
    void roleAssignmentMustBeConfirmedByKeycloak() {
        RoleSetup setup = roleSetup();
        RoleRepresentation ownerRole = new RoleRepresentation();
        ownerRole.setName("OWNER");
        when(setup.realmRoles.get("OWNER")).thenReturn(setup.roleResource);
        when(setup.roleResource.toRepresentation()).thenReturn(ownerRole);
        when(setup.roleScope.listAll()).thenReturn(List.of(ownerRole));

        setup.adapter.assignRealmRole("user-id", "OWNER");

        verify(setup.roleScope).add(eq(List.of(ownerRole)));
        verify(setup.roleScope).listAll();
    }

    @Test
    void roleAssignmentFailsWhenKeycloakDoesNotReturnTheGrantedRole() {
        RoleSetup setup = roleSetup();
        RoleRepresentation ownerRole = new RoleRepresentation();
        ownerRole.setName("OWNER");
        when(setup.realmRoles.get("OWNER")).thenReturn(setup.roleResource);
        when(setup.roleResource.toRepresentation()).thenReturn(ownerRole);
        when(setup.roleScope.listAll()).thenReturn(List.of());

        assertThrows(WebApplicationException.class,
                () -> setup.adapter.assignRealmRole("user-id", "OWNER"));
    }

    @Test
    void roleAssignmentPropagatesKeycloakWriteFailures() {
        RoleSetup setup = roleSetup();
        RoleRepresentation ownerRole = new RoleRepresentation();
        ownerRole.setName("OWNER");
        when(setup.realmRoles.get("OWNER")).thenReturn(setup.roleResource);
        when(setup.roleResource.toRepresentation()).thenReturn(ownerRole);
        doThrow(new WebApplicationException("Keycloak role assignment failed"))
                .when(setup.roleScope).add(any());

        assertThrows(WebApplicationException.class,
                () -> setup.adapter.assignRealmRole("user-id", "OWNER"));

        verify(setup.roleScope, never()).listAll();
    }

    private RoleSetup roleSetup() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        RolesResource realmRoles = mock(RolesResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappings = mock(RoleMappingResource.class);
        RoleScopeResource roleScope = mock(RoleScopeResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        when(keycloak.realm("handwerker-realm")).thenReturn(realmResource);
        when(realmResource.roles()).thenReturn(realmRoles);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-id")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappings);
        when(roleMappings.realmLevel()).thenReturn(roleScope);
        return new RoleSetup(
                new KeycloakUserAdapter(keycloak, "handwerker-realm"),
                realmRoles,
                roleScope,
                roleResource
        );
    }

    private static class RoleSetup {
        private final KeycloakUserAdapter adapter;
        private final RolesResource realmRoles;
        private final RoleScopeResource roleScope;
        private final RoleResource roleResource;

        private RoleSetup(
                KeycloakUserAdapter adapter,
                RolesResource realmRoles,
                RoleScopeResource roleScope,
                RoleResource roleResource
        ) {
            this.adapter = adapter;
            this.realmRoles = realmRoles;
            this.roleScope = roleScope;
            this.roleResource = roleResource;
        }
    }
}
