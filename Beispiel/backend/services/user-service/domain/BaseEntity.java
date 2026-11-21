/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS:
 *   backend/services/user-service/src/main/java/de/winfprojekt/craftvoice/userservice/BaseEntity.java
 * NEUE SCHICHT: domain  (gemeinsame Basisklasse von UserEntity und AuditLogEntity)
 * -------------------------------------------------------------------------------------
 * WAS JETZT IN DIESER DATEI ZU TUN IST:
 *
 * 1. Package anpassen (bewusst noch NICHT geändert):
 *      de.winfprojekt.craftvoice.userservice -> de.winfprojekt.craftvoice.userservice.domain
 *    infrastructure/AuditLogEntity.java muss diese Klasse dann importieren.
 *
 * 2. Die Klasse ist rein technisch (Panache-ID + created/updated-Zeitstempel). Liegt hier,
 *    solange UserEntity selbst eine JPA-Entity ist. Wird die Domain JPA-frei gemacht
 *    (siehe Punkt 2 in domain/UserEntity.java), diese Datei nach infrastructure/ verschieben.
 * =====================================================================================
 */
package de.winfprojekt.craftvoice.userservice;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    void onPersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
