package de.winfprojekt.werkmundo.userservice.domain;

import java.util.Optional;
import java.util.UUID;

/** Zugriff auf Betriebe samt Bankkonten, Lohngruppen, Texten und Einstellungen. */
public interface CompanyRepository {

    Optional<Company> findById(UUID id);

    /** Speichert einen neuen Betrieb inklusive aller angehängten Daten. */
    void add(Company company);
}
