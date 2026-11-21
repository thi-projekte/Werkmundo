/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS:
 *   backend/services/user-service/src/main/java/de/winfprojekt/craftvoice/userservice/AuditLogEntity.java
 * NEUE SCHICHT: infrastructure  (technisches Audit-Log, Persistenz in Tabelle audit_logs)
 * -------------------------------------------------------------------------------------
 * WAS JETZT IN DIESER DATEI ZU TUN IST:
 *
 * 1. Package anpassen (bewusst noch NICHT geändert):
 *      de.winfprojekt.craftvoice.userservice -> de.winfprojekt.craftvoice.userservice.infrastructure
 *    und Import auf ...userservice.domain.BaseEntity ergänzen.
 *
 * 2. Die statische Methode log(...) wird direkt aus application/UserService.java aufgerufen.
 *    Dadurch hängt die application-Schicht fest an dieser JPA-Klasse.
 *    -> Interface anlegen, z.B. application/port/AuditLog.java mit
 *       void log(Long userId, String action, String description);
 *       und hier (oder in infrastructure/JpaAuditLog.java) als @ApplicationScoped-Bean
 *       implementieren. UserService injiziert dann das Interface.
 *
 * 3. Ungenutzten Import io.quarkus.hibernate.orm.panache.PanacheEntity entfernen
 *    (die Klasse erbt von BaseEntity, nicht direkt von PanacheEntity).
 *
 * 4. "action" ist ein freier String -> optional Enum (z.B. AuditAction) einführen.
 * =====================================================================================
 */
package de.winfprojekt.craftvoice.userservice;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity extends BaseEntity {

    public Long userId;
    public String action; // e.g., "PASSWORD_CHANGE", "COMPANY_DATA_UPDATE"
    public String description;

    public static void log(Long userId, String action, String description) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.userId = userId;
        entry.action = action;
        entry.description = description;
        entry.persist();
    }
}
