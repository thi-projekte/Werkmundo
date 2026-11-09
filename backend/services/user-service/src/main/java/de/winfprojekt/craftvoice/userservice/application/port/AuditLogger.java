package de.winfprojekt.craftvoice.userservice.application.port;

public interface AuditLogger {

    void log(Long userId, String action, String description);
}
