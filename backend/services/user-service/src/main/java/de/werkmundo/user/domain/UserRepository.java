package de.werkmundo.user.domain;

import de.werkmundo.user.UserEntity;

public interface UserRepository {

    UserEntity findByEmail(String email);

    UserEntity findByNormalizedEmail(String normalizedEmail);

    UserEntity findByKeycloakId(String keycloakId);

    UserEntity findById(Long id);

    void persistUser(UserEntity user);
}
