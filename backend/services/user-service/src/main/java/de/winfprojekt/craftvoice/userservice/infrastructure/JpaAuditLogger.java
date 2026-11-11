package de.winfprojekt.craftvoice.userservice.infrastructure;

import de.winfprojekt.craftvoice.userservice.application.port.AuditLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class JpaAuditLogger implements AuditLogger {

    @Override
    @Transactional
    public void log(Long userId, String action, String description) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.userId = userId;
        entry.action = action;
        entry.description = description;
        entry.persist();
    }
}
