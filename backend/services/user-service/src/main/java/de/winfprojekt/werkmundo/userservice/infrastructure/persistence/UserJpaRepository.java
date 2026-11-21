package de.winfprojekt.werkmundo.userservice.infrastructure.persistence;

import de.winfprojekt.werkmundo.userservice.domain.User;
import de.winfprojekt.werkmundo.userservice.domain.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** JPA-Umsetzung von {@link UserRepository}. */
@ApplicationScoped
public class UserJpaRepository implements UserRepository {

    private final EntityManager entityManager;

    public UserJpaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(User.class, id));
    }

    @Override
    public Optional<User> findByKeycloakId(String keycloakId) {
        return entityManager.createQuery("select u from User u where u.keycloakId = :keycloakId", User.class)
                .setParameter("keycloakId", keycloakId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return entityManager.createQuery("select u from User u where u.email = :email", User.class)
                .setParameter("email", normalize(email))
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<User> findByCompany(UUID companyId) {
        return entityManager.createQuery(
                        "select u from User u where u.company.id = :companyId order by u.lastName, u.firstName", User.class)
                .setParameter("companyId", companyId)
                .getResultList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return entityManager.createQuery("select count(u) from User u where u.email = :email", Long.class)
                .setParameter("email", normalize(email))
                .getSingleResult() > 0;
    }

    @Override
    public void add(User user) {
        entityManager.persist(user);
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
