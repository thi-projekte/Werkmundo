package de.werkmundo.user.infrastructure;

import de.werkmundo.user.UserEntity;
import de.werkmundo.user.domain.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserJpaRepository implements PanacheRepository<UserEntity>, UserRepository {

    @Override
    public UserEntity findByEmail(String email) {
        return find("email", email).firstResult();
    }

    @Override
    public UserEntity findByNormalizedEmail(String normalizedEmail) {
        return find("lower(email) = ?1", normalizedEmail).firstResult();
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
