package de.winfprojekt.craftvoice.userservice.infrastructure;

import de.winfprojekt.craftvoice.userservice.UserEntity;
import de.winfprojekt.craftvoice.userservice.domain.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserJpaRepository implements PanacheRepository<UserEntity>, UserRepository {

    @Override
    public UserEntity findByEmail(String email) {
        return find("email", email).firstResult();
    }

    @Override
    public UserEntity findByKeycloakId(String keycloakId) {
        return find("keycloakId", keycloakId).firstResult();
    }

    @Override
    public UserEntity findById(Long id) {
        return find("id", id).firstResult();
    }

    @Override
    public void persistUser(UserEntity user) {
        persist(user);
    }
}
