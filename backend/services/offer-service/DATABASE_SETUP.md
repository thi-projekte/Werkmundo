# Offer-Service – Datenbank und lokales Setup (Issue #32)

Stand: 08.11.2026. Diese Dokumentation beschreibt die PostgreSQL-Persistenz des Offer-Service auf Basis von Arnes Service-Stand `3928208` und der Issue-32-Integration. Änderungen an der Domain-Wiederherstellung (`Offer.restore`) sind mit Arne abzustimmen, bevor die Integration ins Squad erfolgt.

## Voraussetzungen

- Docker Desktop mit laufender Docker Engine
- Java 25 und Maven
- Freier lokaler TCP-Port `55432`

## Lokale PostgreSQL-Datenbank starten (PowerShell)

Die Befehle aus dem Verzeichnis `backend/services/offer-service` ausführen:

```powershell
$env:OFFER_DB_PASSWORD = (New-Object System.Net.NetworkCredential("", (Read-Host "Offer DB Password" -AsSecureString))).Password
docker compose -f .\docker-compose.db.yml up -d
docker compose -f .\docker-compose.db.yml ps
```

Der lokale Compose-Stack verwendet `postgres:17-alpine`, Datenbank `offer_db`, Benutzer `offer_user`, den Host-Port `127.0.0.1:55432` und das persistente Volume `offer_pgdata`. Das Kennwort gehört ausschließlich in die lokale Umgebungsvariable und **nicht** ins Repository.

Beim ersten Start erstellt Flyway automatisch das Schema (Migration `src/main/resources/db/migration/V1__create_offer_tables.sql`), sofern die Datenbankaktivierung wie unten gesetzt ist. Ein bereits angewendetes `V1`-Skript darf nachträglich nicht verändert werden; spätere Schemaänderungen erhalten eine neue nummerierte Migration.

## Lokalen Offer-Service mit PostgreSQL ausführen

In **derselben** PowerShell-Sitzung:

```powershell
$env:OFFER_PERSISTENCE = "postgres"
$env:OFFER_DB_ENABLED = "true"
# OFFER_DB_PASSWORD muss bereits gesetzt sein (siehe oben).
mvn quarkus:dev
```

Ohne `OFFER_PERSISTENCE=postgres` nutzt der lokale Service standardmäßig Arnes In-Memory-Implementierung. Ohne `OFFER_DB_ENABLED=true` bleiben Hibernate ORM und Flyway in den Quarkus-Profilen `dev` und `test` deaktiviert. In `prod` ist PostgreSQL aktiviert und die In-Memory-Implementierung nicht vorgesehen.

Konfigurierbare Variablen in `src/main/resources/application.properties`:

| Variable | Standard (lokal) | Zweck |
| --- | --- | --- |
| `OFFER_PERSISTENCE` | `memory` | `postgres` aktiviert `OfferJpaRepository` |
| `OFFER_DB_ENABLED` | `false` (dev/test) | ORM und Flyway in dev/test aktivieren |
| `OFFER_DB_JDBC_URL` | `jdbc:postgresql://localhost:55432/offer_db` | JDBC-Verbindung |
| `OFFER_DB_USER` | `offer_user` | DB-Benutzer |
| `OFFER_DB_PASSWORD` | leer | DB-Kennwort, zur Laufzeit setzen |

Für eine Ausführung in Docker bzw. eine gemeinsame Service-Umgebung müssen JDBC-Host, Zugangsdaten und Netzwerkkonfiguration explizit auf die dortige Datenbank angepasst werden. `localhost:55432` ist nur für die beschriebene lokale Ausführung auf dem Host gedacht.

## Schema und Beziehungen

Die erste Flyway-Migration legt fünf Tabellen an:

| Tabelle | Inhalt | Beziehung |
| --- | --- | --- |
| `offers` | Angebots-ID, eindeutiger `business_key`, Kunden-/Handwerker-ID, Token-Hash und Ablauf, geschätzte Dauer, Status, Zeitstempel | Wurzeltabelle |
| `offer_positions` | Angebotspositionen inkl. Typ, Menge, Einzelpreis und Reihenfolge | `offer_id` → `offers.id` |
| `offer_status_history` | Statuswechsel mit Zeitstempel | `offer_id` → `offers.id` |
| `offer_correction_suggestions` | Geordnete Korrekturhinweise | `offer_id` → `offers.id` |
| `offer_customer_change_requests` | Geordnete Kundenänderungswünsche | `offer_id` → `offers.id` |

Alle vier Kindtabellen referenzieren `offers.id` mit `ON DELETE CASCADE`. Der fachliche Schlüssel `business_key` ist eindeutig. Die Relationen werden über JPA (`OfferEntity` und zugehörige Entities) abgebildet; das Domain-Repository wird durch `OfferJpaRepository` mit `save`, `findByBusinessKey` und `findAllByCraftsmanId` angebunden.

`totalPrice` wird anhand der Positionen berechnet, nicht als eigene Spalte gespeichert. Referenzen auf Kunden, Handwerker und Katalogprodukte werden als Fremd-IDs gehalten; keine serviceübergreifenden SQL-Foreign-Keys.

## Tests

Mit laufender PostgreSQL-Datenbank und gesetzten Variablen (`OFFER_DB_PASSWORD`, `OFFER_PERSISTENCE=postgres`, `OFFER_DB_ENABLED=true`):

```powershell
mvn clean "-Dtest=OfferJpaRepositoryIntegrationTest" "-Dquarkus.http.test-port=18081" test
mvn "-Dquarkus.http.test-port=18081" test
```

Im lokalen Integration-Worktree am 08.11.2026 erfolgreich ausgeführt: **57 Tests, 0 Failures, 0 Errors, 0 Skipped, BUILD SUCCESS**. Der Integrations-Test verwendet `@TestTransaction` (Rollback) und prüft Speichern, Laden, Positionen, Statushistorie, Statusänderung und Handwerker-Abfrage über das Domain-Repository. Dies ist kein Nachweis für das Deployment am gemeinsamen API Gateway.

## Datenbank stoppen

```powershell
docker compose -f .\docker-compose.db.yml down
```

Das benannte Volume bleibt erhalten. **Achtung:** `down -v` würde auch die gespeicherten Daten des Stacks entfernen und sollte nur für einen bewusst gewünschten Reset verwendet werden.

## Integration und Grenzen

- Der Integration-Worktree basiert auf Arnes Branch `Arne/31_Offer-Service_implementieren` (`3928208`), nicht auf einem bereits gemergten Stand von `SquadAuftraggeber`.
- Die Domain-Erweiterung für Rehydration und die Umschaltung zwischen In-Memory- und PostgreSQL-Repository müssen vor dem Merge mit Arne/Squad abgestimmt werden.
- API-Gateway-Anbindung, Deployment-Compose und die eigenständige Datenbank des Document-Service (Issue #34) sind **nicht** Bestandteil dieses lokalen PostgreSQL-Setups.
- Rechnungs-/Angebots-PDF und Geschäftsnummern sind separate Aufgaben.
