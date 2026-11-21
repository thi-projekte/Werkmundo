package de.winfprojekt.werkmundo.userservice.infrastructure.persistence;

import de.winfprojekt.werkmundo.userservice.domain.Company;
import de.winfprojekt.werkmundo.userservice.domain.CompanyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;

/** JPA-Umsetzung von {@link CompanyRepository}. */
@ApplicationScoped
public class CompanyJpaRepository implements CompanyRepository {

    private final EntityManager entityManager;

    public CompanyJpaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(Company.class, id));
    }

    @Override
    public void add(Company company) {
        entityManager.persist(company);
    }
}
