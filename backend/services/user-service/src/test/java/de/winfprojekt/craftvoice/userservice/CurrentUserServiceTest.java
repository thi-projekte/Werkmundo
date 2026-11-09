package de.winfprojekt.craftvoice.userservice;

import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class CurrentUserServiceTest {

    private JsonWebToken jwt;
    private UserRepository userRepository;
    private IdentityProvider identityProvider;
    private CurrentUserService service;

    @BeforeEach
    void setUp() {
        jwt = mock(JsonWebToken.class);
        userRepository = mock(UserRepository.class);
        identityProvider = mock(IdentityProvider.class);
        service = new CurrentUserService(jwt, userRepository, identityProvider);
        when(jwt.getSubject()).thenReturn("keycloak-user");
        when(jwt.getClaim("email")).thenReturn("user@example.com");
    }

    @Test
    void unknownSubjectsAreNotAutomaticallyProvisionedAsOwners() {
        when(userRepository.findByKeycloakId("keycloak-user")).thenReturn(null);

        assertThrows(NotFoundException.class, service::syncUserWithDatabase);

        verify(userRepository, never()).persistUser(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(identityProvider);
    }

    @Test
    void pendingUserCannotSyncBeforeEmailVerification() {
        UserEntity user = pendingUser();
        when(userRepository.findByKeycloakId("keycloak-user")).thenReturn(user);
        when(jwt.getClaim("email_verified")).thenReturn(Boolean.FALSE);

        assertThrows(ForbiddenException.class, service::syncUserWithDatabase);

        assertEquals(UserStatus.PENDING, user.status);
        verifyNoInteractions(identityProvider);
    }

    @Test
    void verifiedPendingUserWithMatchingNormalizedEmailIsActivatedAndGrantedOwnerRole() {
        UserEntity user = pendingUser();
        user.email = " User@Example.com ";
        when(userRepository.findByKeycloakId("keycloak-user")).thenReturn(user);
        when(jwt.getClaim("email")).thenReturn("user@example.COM");
        when(jwt.getClaim("email_verified")).thenReturn(Boolean.TRUE);

        UserEntity result = service.syncUserWithDatabase();

        assertEquals(user, result);
        assertEquals(UserStatus.ACTIVE, user.status);
        assertEquals(java.util.Set.of(UserRole.OWNER), user.roles);
        assertEquals(" User@Example.com ", user.email);
        verify(identityProvider).assignRealmRole("keycloak-user", "OWNER");
    }

    @Test
    void verifiedPendingUserWithDifferentEmailIsNotActivatedOrGrantedOwnerRole() {
        UserEntity user = pendingUser();
        when(userRepository.findByKeycloakId("keycloak-user")).thenReturn(user);
        when(jwt.getClaim("email")).thenReturn("attacker@example.com");
        when(jwt.getClaim("email_verified")).thenReturn(Boolean.TRUE);

        assertThrows(ForbiddenException.class, service::syncUserWithDatabase);

        assertEquals(UserStatus.PENDING, user.status);
        assertEquals(java.util.Set.of(), user.roles);
        assertEquals("old@example.com", user.email);
        verifyNoInteractions(identityProvider);
    }

    @Test
    void keycloakRoleAssignmentFailureLeavesPendingUserUnchanged() {
        UserEntity user = pendingUser();
        when(userRepository.findByKeycloakId("keycloak-user")).thenReturn(user);
        when(jwt.getClaim("email")).thenReturn("old@example.com");
        when(jwt.getClaim("email_verified")).thenReturn(Boolean.TRUE);
        doThrow(new IllegalStateException("Role assignment failed"))
                .when(identityProvider).assignRealmRole("keycloak-user", "OWNER");

        assertThrows(IllegalStateException.class, service::syncUserWithDatabase);

        assertEquals(UserStatus.PENDING, user.status);
        assertEquals(java.util.Set.of(), user.roles);
        assertEquals("old@example.com", user.email);
    }

    @Test
    void activeUsersDoNotReceiveRolesFromLocalSynchronization() {
        UserEntity user = pendingUser();
        user.status = UserStatus.ACTIVE;
        user.roles.clear();
        when(userRepository.findByKeycloakId("keycloak-user")).thenReturn(user);
        when(jwt.<String>getClaim("given_name")).thenReturn("Ada");
        when(jwt.<String>getClaim("family_name")).thenReturn("Lovelace");

        UserEntity result = service.syncUserWithDatabase();

        assertEquals(user, result);
        assertEquals("Ada", user.firstName);
        assertEquals("Lovelace", user.lastName);
        assertEquals(0, user.roles.size());
        verifyNoInteractions(identityProvider);
    }

    private UserEntity pendingUser() {
        UserEntity user = new UserEntity();
        user.keycloakId = "keycloak-user";
        user.email = "old@example.com";
        user.status = UserStatus.PENDING;
        return user;
    }
}
