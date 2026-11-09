package de.winfprojekt.craftvoice.userservice.application;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.UserRole;
import de.winfprojekt.craftvoice.userservice.UserStatus;
import de.winfprojekt.craftvoice.userservice.application.port.AuditLogger;
import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    @Test
    void registerKeepsTheUserPendingWithoutAssigningRoles() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(identityProvider.createUser("new@example.com", "Ada", "Lovelace", "password123"))
                .thenReturn("keycloak-23");
        RegistrationService service = new RegistrationService(identityProvider, userRepository, auditLogger);

        UserEntity user = new UserEntity();
        user.email = "new@example.com";
        user.firstName = "Ada";
        user.lastName = "Lovelace";
        user.status = UserStatus.ACTIVE;
        user.roles.add(UserRole.ACCOUNTANT);
        user.roles.add(UserRole.CUSTOMER);

        service.register(user, "password123");

        assertEquals(UserStatus.PENDING, user.status);
        assertEquals(java.util.Set.of(), user.roles);
        assertEquals("keycloak-23", user.keycloakId);
        verify(userRepository).persistUser(user);
    }

    @Test
    void registerRejectsAnAlreadyRegisteredEmail() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity existingUser = new UserEntity();
        when(userRepository.findByEmail("existing@example.com")).thenReturn(existingUser);
        RegistrationService service = new RegistrationService(identityProvider, userRepository, auditLogger);

        UserEntity user = new UserEntity();
        user.email = "existing@example.com";

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.register(user, "password")
        );

        assertEquals("Email already registered", exception.getMessage());
        verifyNoInteractions(identityProvider);
    }

    @Test
    void registerRejectsAKeycloakCreationFailure() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(identityProvider.createUser("new@example.com", null, null, "password"))
                .thenThrow(new BadRequestException("Could not create user in Keycloak: creation rejected"));
        RegistrationService service = new RegistrationService(identityProvider, userRepository, auditLogger);

        UserEntity user = new UserEntity();
        user.email = "new@example.com";

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.register(user, "password")
        );

        assertEquals("Could not create user in Keycloak: creation rejected", exception.getMessage());
        verify(userRepository).findByEmail("new@example.com");
        verifyNoMoreInteractions(userRepository);
        verify(identityProvider).createUser("new@example.com", null, null, "password");
        verifyNoMoreInteractions(identityProvider);
    }

    @Test
    void deleteAccountRejectsAnUnknownUser() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findById(42L)).thenReturn(null);
        RegistrationService service = new RegistrationService(identityProvider, userRepository, auditLogger);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.deleteAccount(42L)
        );

        assertEquals("User not found", exception.getMessage());
        verifyNoInteractions(identityProvider);
    }

    @Test
    void passwordResetDelegatesToIdentityProvider() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        RegistrationService service = new RegistrationService(
                identityProvider, mock(UserRepository.class), mock(AuditLogger.class)
        );

        service.initiatePasswordReset("user@example.com");

        verify(identityProvider).sendPasswordResetEmail("user@example.com");
        verifyNoMoreInteractions(identityProvider);
    }

    @Test
    void registerKeepsAccountPendingWithoutGrantingTheRealmOwnerRole() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findByEmail("new@example.com")).thenReturn(null);
        when(identityProvider.createUser("new@example.com", "Ada", "Lovelace", "registration-password"))
                .thenReturn("keycloak-23");
        RegistrationService service = new RegistrationService(identityProvider, userRepository, auditLogger);

        UserEntity user = new UserEntity();
        user.id = 23L;
        user.email = "new@example.com";
        user.firstName = "Ada";
        user.lastName = "Lovelace";

        service.register(user, "registration-password");

        assertEquals(UserStatus.PENDING, user.status);
        assertEquals(java.util.Set.of(), user.roles);
        verify(auditLogger).log(23L, "REGISTRATION", "User registered via Keycloak");
        verify(identityProvider).sendVerificationEmail("keycloak-23");
        verify(identityProvider, org.mockito.Mockito.never()).assignRealmRole("keycloak-23", "OWNER");
        verify(auditLogger, org.mockito.Mockito.never())
                .log(eq(23L), eq("REGISTRATION"), contains("registration-password"));
    }

    @Test
    void deleteAccountLogsTheExistingAuditEvent() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity user = new UserEntity();
        user.id = 42L;
        user.keycloakId = "keycloak-42";
        when(userRepository.findById(42L)).thenReturn(user);
        RegistrationService service = new RegistrationService(identityProvider, userRepository, auditLogger);

        service.deleteAccount(42L);

        verify(auditLogger).log(42L, "ACCOUNT_DELETED", "User account deleted and anonymized locally");
    }
}
