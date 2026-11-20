package de.werkmundo.user.application.port;

public interface AuditLogger {

    void log(Long userId, String action, String description);
}
