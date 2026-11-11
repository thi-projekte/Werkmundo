package de.winfprojekt.craftvoice.userservice.domain;

import de.winfprojekt.craftvoice.userservice.UserEntity;

public interface UserRepository {

    UserEntity findByEmail(String email);

    UserEntity findByKeycloakId(String keycloakId);

    UserEntity findById(Long id);

    void persistUser(UserEntity user);
}
