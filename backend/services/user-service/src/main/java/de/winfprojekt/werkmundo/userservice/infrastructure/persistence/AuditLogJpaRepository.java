package de.winfprojekt.werkmundo.userservice.infrastructure.persistence;

import de.winfprojekt.werkmundo.userservice.domain.AuditLogEntry;
import de.winfprojekt.werkmundo.userservice.domain.AuditLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

/** JPA-Umsetzung von {@link AuditLogRepository}. */
@ApplicationScoped
public class AuditLogJpaRepository implements AuditLogRepository {

    private final EntityManager entityManager;

    public AuditLogJpaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(AuditLogEntry entry) {
        entityManager.persist(entry);
    }

    @Override
    public List<AuditLogEntry> findLatestByCompany(UUID companyId, int limit) {
        return entityManager.createQuery(
                        "select a from AuditLogEntry a where a.companyId = :companyId order by a.createdAt desc",
                        AuditLogEntry.class)
                .setParameter("companyId", companyId)
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }
}
