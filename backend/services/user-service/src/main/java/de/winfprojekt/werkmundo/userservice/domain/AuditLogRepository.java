package de.winfprojekt.werkmundo.userservice.domain;

import java.util.List;
import java.util.UUID;

/** Schreibt und liest das Änderungsprotokoll. */
public interface AuditLogRepository {

    void add(AuditLogEntry entry);

    /** Neueste Einträge eines Betriebs zuerst. */
    List<AuditLogEntry> findLatestByCompany(UUID companyId, int limit);
}
