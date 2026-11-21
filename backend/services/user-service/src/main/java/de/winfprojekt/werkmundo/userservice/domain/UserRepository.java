package de.winfprojekt.werkmundo.userservice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Zugriff auf Benutzerkonten. Änderungen an geladenen Objekten werden in der laufenden Transaktion gespeichert. */
public interface UserRepository {

    Optional<User> findById(UUID id);

    /** Sucht über den Claim {@code sub} des Keycloak-Tokens. */
    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByEmail(String email);

    List<User> findByCompany(UUID companyId);

    boolean existsByEmail(String email);

    /** Speichert einen neuen Benutzer. Sein Betrieb muss bereits gespeichert sein oder im selben Schritt gespeichert werden. */
    void add(User user);
}
