# Offer-Service – Datenbank und lokales Setup (Issue #32)

Stand: 08.11.2026. Diese Dokumentation beschreibt die PostgreSQL-Persistenz des Offer-Service auf Basis von Arnes Service-Stand `3928208` und der Issue-32-Integration. Änderungen an der Domain-Wiederherstellung (`Offer.restore`) sind mit Arne abzustimmen, bevor die Integration ins Squad erfolgt.

## Voraussetzungen

- Docker Desktop mit laufender Docker Engine
- Java 25 und Maven
- Freier lokaler TCP-Port `55432`

## Offer-Service und CRUD manuell testen (Windows PowerShell)

Die folgenden Schritte starten PostgreSQL und den Service lokal und testen anschließend Create, Read, Update und Delete per HTTP. Docker Desktop muss laufen; Java 25 ist erforderlich. Der Service und die Datenbank laufen in getrennten Fenstern. Verwende für die Befehle die PowerShell-Syntax.

### 1. PostgreSQL starten

Öffne PowerShell-Fenster 1 und wechsle in das Service-Verzeichnis:

```powershell
Set-Location 'C:\Users\Arnem\OneDrive\Dokumente\Studium\Master\Inhalt\1_Semester\Werkmundo\repo\Werkmundo\backend\services\offer-service'
```

Setze Java 25 und gib ein lokales Datenbankkennwort ein:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:OFFER_DB_PASSWORD = (New-Object System.Net.NetworkCredential("", (Read-Host "Offer DB Password" -AsSecureString))).Password
```

Starte den Datenbank-Container und prüfe, ob er `healthy` ist:

```powershell
docker compose -f .\docker-compose.db.yml up -d
docker compose -f .\docker-compose.db.yml ps
```

Der Stack verwendet PostgreSQL 17, Datenbank `offer_db`, Benutzer `offer_user`, Host-Port `127.0.0.1:55432` und das persistente Volume `offer_pgdata`. Das Kennwort bleibt in der lokalen Umgebungsvariable und gehört nicht ins Repository.

### 2. Offer-Service mit PostgreSQL starten

Im selben Fenster 1 aktivierst du PostgreSQL und das lokale API-Testkonto. Der Benutzername ist `offer-dev`; dessen Passwort legst du bei der Eingabe selbst fest:

```powershell
$env:OFFER_PERSISTENCE = 'postgres'
$env:OFFER_DB_ENABLED = 'true'
$env:OFFER_DEV_PASSWORD = (New-Object System.Net.NetworkCredential("", (Read-Host "Lokales Offer-API-Passwort" -AsSecureString))).Password
$env:PORT = '18080'
.\mvnw.cmd quarkus:dev
```

Lass das Fenster geöffnet. Beim ersten Start legt Flyway das Schema aus `src/main/resources/db/migration/V1__create_offer_tables.sql` an. Port `18080` wird hier absichtlich gewählt, damit ein anderer Dienst auf Port `8080` nicht stört. Im `dev`-Profil bindet die lokale Basic Auth nur an `127.0.0.1`; sie ist ausschließlich für lokale Tests gedacht, nicht für Produktion.

### 3. Basic-Auth-Zugang in einer zweiten PowerShell setzen

Öffne PowerShell-Fenster 2. Bei der Passwortabfrage gibst du dasselbe lokale Offer-API-Passwort ein wie in Fenster 1:

```powershell
$credential = Get-Credential -UserName 'offer-dev' -Message 'Lokaler Offer-Service'
$pair = "$($credential.UserName):$($credential.GetNetworkCredential().Password)"
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{ Authorization = "Basic $basic" }
$baseUrl = 'http://127.0.0.1:18080'
```

### 4. Create – Angebot anlegen

`businessKey` muss eindeutig sein. Falls du dieses Beispiel erneut ausführst, ändere den Wert, da er in der Datenbank eindeutig ist:

```powershell
$body = @{
    businessKey = 'manual-test-1'
    customerId = 'customer-1'
    correctionSuggestions = @()
    positions = @()
} | ConvertTo-Json

$created = Invoke-RestMethod -Method Post -Uri "$baseUrl/offers" `
    -Headers $headers -ContentType 'application/json' -Body $body
$created | Format-List
```

Erwartet: HTTP `201 Created` und ein Angebotsobjekt mit `businessKey` `manual-test-1`.

### 5. Read – Angebot abrufen

```powershell
$offerUrl = "$baseUrl/offers/manual-test-1"
Invoke-RestMethod -Method Get -Uri $offerUrl -Headers $headers | Format-List
```

Erwartet: Das zuvor angelegte Angebot mit Status `DRAFT`.

### 6. Update – Angebot ändern

```powershell
$update = @{
    customerId = 'customer-1'
    correctionSuggestions = @('Manuell aktualisiert')
    positions = @()
} | ConvertTo-Json

Invoke-RestMethod -Method Put -Uri $offerUrl -Headers $headers `
    -ContentType 'application/json' -Body $update | Format-List
```

Rufe es erneut ab, um die Änderung zu prüfen:

```powershell
Invoke-RestMethod -Method Get -Uri $offerUrl -Headers $headers | Format-List
```

Erwartet: `correctionSuggestions` enthält `Manuell aktualisiert`.

### 7. Delete – Angebot löschen

```powershell
$response = Invoke-WebRequest -Method Delete -Uri $offerUrl -Headers $headers
$response.StatusCode
```

Erwartet: `204`. Ein weiterer `GET` auf dieselbe URL liefert `404`, weil das Angebot gelöscht wurde. Bei Erfolg werden auch abhängige Angebotsdaten aus der Datenbank entfernt.

### 8. Datenbank stoppen

Wenn du fertig bist, beende den Service mit `Ctrl+C` in Fenster 1 und stoppe anschließend den Datenbank-Container:

```powershell
docker compose -f .\docker-compose.db.yml down
```

Das benannte Volume bleibt erhalten. `down -v` würde auch die gespeicherten Daten löschen und sollte nur für einen bewusst gewünschten Reset verwendet werden.

Ohne `OFFER_PERSISTENCE=postgres` nutzt der lokale Service standardmäßig die In-Memory-Implementierung. Ohne `OFFER_DB_ENABLED=true` bleiben Hibernate ORM und Flyway in `dev` und `test` deaktiviert. In `prod` ist PostgreSQL aktiviert; die hier beschriebene lokale Embedded-Authentifizierung gilt dort nicht.

Konfigurierbare Variablen in `src/main/resources/application.properties`:

| Variable | Standard (lokal) | Zweck |
| --- | --- | --- |
| `OFFER_PERSISTENCE` | `memory` | `postgres` aktiviert `OfferJpaRepository` |
| `OFFER_DB_ENABLED` | `false` (dev/test) | ORM und Flyway in dev/test aktivieren |
| `OFFER_DB_JDBC_URL` | `jdbc:postgresql://localhost:55432/offer_db` | JDBC-Verbindung |
| `OFFER_DB_USER` | `offer_user` | DB-Benutzer |
| `OFFER_DB_PASSWORD` | leer | DB-Kennwort, zur Laufzeit setzen |
| `OFFER_DEV_PASSWORD` | nicht gesetzt | Passwort des lokalen Basic-Auth-Kontos `offer-dev` im `dev`-Profil |
| `PORT` | `8080` | HTTP-Port des Services; die Anleitung verwendet `18080` |

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

Alle vier Kindtabellen referenzieren `offers.id` mit `ON DELETE CASCADE`. Der fachliche Schlüssel `business_key` ist eindeutig. Die Relationen werden über JPA (`OfferEntity` und zugehörige Entities) abgebildet; das Domain-Repository wird durch `OfferJpaRepository` mit `save`, `findByBusinessKey`, `findAllByCraftsmanId` und `deleteByBusinessKey` angebunden. Beim Löschen eines Angebots werden seine abhängigen Datensätze mit entfernt.

Die API unterstützt `DELETE /offers/{businessKey}`. Erfolgreiches Löschen liefert `204 No Content`; wie bei `GET` und `PUT` darf nur der authentifizierte Handwerker sein eigenes Angebot löschen.

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
