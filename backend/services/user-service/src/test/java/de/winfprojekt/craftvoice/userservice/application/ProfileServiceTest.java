package de.winfprojekt.craftvoice.userservice.application;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.application.port.AuditLogger;
import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import de.winfprojekt.craftvoice.userservice.application.port.ProfilePictureStore;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProfileServiceTest {

    Path tempDir;

    @BeforeEach
    void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("profile-service-test-");
    }

    @AfterEach
    void deleteTempDir() throws IOException {
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Test
    @TestTransaction
    void updateProfileChangesOnlyTheRepositoryUserAndUsesIdentityProvider() {
        UserRepository userRepository = mock(UserRepository.class);
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity storedUser = new UserEntity();
        storedUser.id = 17L;
        storedUser.keycloakId = "keycloak-17";
        when(userRepository.findById(17L)).thenReturn(storedUser);
        ProfileService service = new ProfileService(
                userRepository, identityProvider, mock(ProfilePictureStore.class), auditLogger
        );

        UserEntity data = new UserEntity();
        data.id = 99L;
        data.firstName = "Ada";
        data.lastName = "Lovelace";

        UserEntity result = service.updateProfile(17L, data);

        assertSame(storedUser, result);
        assertEquals(17L, result.id);
        assertEquals("Ada", result.firstName);
        assertEquals("Lovelace", result.lastName);
        verify(userRepository).findById(17L);
        verify(userRepository, never()).findById(99L);
        verify(identityProvider).updateName("keycloak-17", "Ada", "Lovelace");
        verify(auditLogger).log(17L, "PROFILE_UPDATE", "User updated personal data");
    }

    @Test
    @TestTransaction
    void updateProfileKeepsLocalChangesWhenKeycloakSyncFails() {
        UserRepository userRepository = mock(UserRepository.class);
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity storedUser = new UserEntity();
        storedUser.id = 17L;
        storedUser.keycloakId = "keycloak-17";
        when(userRepository.findById(17L)).thenReturn(storedUser);
        org.mockito.Mockito.doThrow(new IllegalStateException("Keycloak unavailable"))
                .when(identityProvider).updateName("keycloak-17", "Ada", null);
        ProfileService service = new ProfileService(
                userRepository, identityProvider, mock(ProfilePictureStore.class), auditLogger
        );

        UserEntity data = new UserEntity();
        data.firstName = "Ada";

        UserEntity result = service.updateProfile(17L, data);

        assertEquals("Ada", result.firstName);
        verify(identityProvider).updateName("keycloak-17", "Ada", null);
    }

    @Test
    void updateProfileRejectsAnUnknownUser() {
        UserRepository userRepository = mock(UserRepository.class);
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findById(99L)).thenReturn(null);
        ProfileService service = new ProfileService(
                userRepository, identityProvider, mock(ProfilePictureStore.class), auditLogger
        );

        assertThrows(NotFoundException.class, () -> service.updateProfile(99L, new UserEntity()));

        verify(identityProvider, never()).updateName(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @TestTransaction
    void uploadProfilePictureDelegatesStorageForTheSelectedUser() {
        UserRepository userRepository = mock(UserRepository.class);
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        ProfilePictureStore profilePictureStore = mock(ProfilePictureStore.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        UserEntity storedUser = new UserEntity();
        storedUser.id = 17L;
        when(userRepository.findById(17L)).thenReturn(storedUser);
        ProfileService service = new ProfileService(
                userRepository, identityProvider, profilePictureStore, auditLogger
        );
        Path uploadedFile = tempDir.resolve("upload.png");
        org.jboss.resteasy.reactive.multipart.FileUpload file =
                mock(org.jboss.resteasy.reactive.multipart.FileUpload.class);
        when(file.uploadedFile()).thenReturn(uploadedFile);
        when(file.fileName()).thenReturn("avatar.png");
        when(profilePictureStore.store(17L, uploadedFile)).thenReturn("profile_17_generated.png");

        String url = service.uploadProfilePicture(17L, file);

        assertEquals("/api/users/profile-picture/profile_17_generated.png", url);
        assertEquals(url, storedUser.profilePictureUrl);
        org.mockito.Mockito.verify(profilePictureStore).store(17L, uploadedFile);
        org.mockito.Mockito.verify(auditLogger).log(
                17L, "PROFILE_PICTURE_UPLOAD", "User uploaded a new profile picture: profile_17_generated.png"
        );
    }

    @Test
    void uploadProfilePictureRejectsAnUnknownUserBeforeReadingTheUpload() {
        UserRepository userRepository = mock(UserRepository.class);
        IdentityProvider identityProvider = mock(IdentityProvider.class);
        ProfilePictureStore profilePictureStore = mock(ProfilePictureStore.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(userRepository.findById(99L)).thenReturn(null);
        ProfileService service = new ProfileService(
                userRepository, identityProvider, profilePictureStore, auditLogger
        );

        assertThrows(
                NotFoundException.class,
                () -> service.uploadProfilePicture(
                        99L,
                        mock(org.jboss.resteasy.reactive.multipart.FileUpload.class)
                )
        );
        verifyNoInteractions(profilePictureStore);
    }
}
