package de.winfprojekt.craftvoice.userservice.application;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.application.port.AuditLogger;
import de.winfprojekt.craftvoice.userservice.application.port.IdentityProvider;
import de.winfprojekt.craftvoice.userservice.application.port.ProfilePictureStore;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@ApplicationScoped
public class ProfileService {

    private static final Logger LOG = Logger.getLogger(ProfileService.class);

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;
    private final ProfilePictureStore profilePictureStore;
    private final AuditLogger auditLogger;

    @Inject
    public ProfileService(
            UserRepository userRepository,
            IdentityProvider identityProvider,
            ProfilePictureStore profilePictureStore,
            AuditLogger auditLogger
    ) {
        this.userRepository = userRepository;
        this.identityProvider = identityProvider;
        this.profilePictureStore = profilePictureStore;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public String uploadProfilePicture(Long userId, FileUpload file) {
        UserEntity user = findUser(userId);
        if (file == null) {
            throw new BadRequestException("Profile picture upload is required");
        }
        String fileName = profilePictureStore.store(userId, file.uploadedFile());
        // No GET route currently serves this URL; keep the existing response until that is addressed separately.
        String url = "/api/users/profile-picture/" + fileName;
        user.profilePictureUrl = url;

        auditLogger.log(
                user.id,
                "PROFILE_PICTURE_UPLOAD",
                "User uploaded a new profile picture: " + fileName
        );

        return url;
    }

    @Transactional
    public UserEntity updateProfile(Long userId, UserEntity data) {
        UserEntity user = findUser(userId);

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
                identityProvider.updateName(user.keycloakId, data.firstName, data.lastName);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Keycloak profile sync failed for user %s", user.id);
        }

        auditLogger.log(
                user.id,
                "PROFILE_UPDATE",
                "User updated personal data"
        );

        return user;
    }

    private UserEntity findUser(Long userId) {
        UserEntity user = userRepository.findById(userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return user;
    }

}
