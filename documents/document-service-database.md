# Document-Service – PostgreSQL Setup (Issue #34)

## 1. Überblick

Der Document-Service verwendet PostgreSQL zur Speicherung von
Angebots- und Rechnungsdokumenten.

Die Datenbankanbindung, das Repository und die erste Flyway-Migration
wurden bereits von Marcel im Branch `marcel/33_Document-Service`
implementiert.

Dieses Dokument beschreibt die unabhängige lokale Überprüfung
der Datenbankintegration im Rahmen von Issue #34.

## 2. Voraussetzungen

- Windows 11 mit PowerShell
- Docker Desktop
- Java JDK 25
- Maven 3.9.12
- PostgreSQL 17 (Docker)

## 3. Datenbankstruktur

Die Migration `V1__create_documents.sql` erstellt die Tabelle
`documents` mit folgenden Informationen:

- Dokument-ID und Eigentümer
- Dokumenttyp (OFFER oder INVOICE)
- Referenz auf Angebot oder Rechnung
- Optionale Kunden-ID und Empfängeradresse
- Dateiname und PDF-Inhalt (bytea)
- Erstellungszeitpunkt

Zusätzlich werden zwei Indizes für eigentümerbezogene Abfragen erstellt.

Flyway verwaltet die Migrationen über `flyway_schema_history`.

## 4. PostgreSQL lokal starten

Für Issue #34 wird eine separate PostgreSQL-Instanz verwendet.

Beim erstmaligen Einrichten:

```powershell
$dbPassword = Read-Host "Lokales Datenbankpasswort"

docker run -d --name werkmundo-issue34-document-db `
  -p 127.0.0.1:55433:5432 `
  -e POSTGRES_DB=document-db `
  -e POSTGRES_USER=document_user `
  -e "POSTGRES_PASSWORD=$dbPassword" `
  -v werkmundo-issue34-document-data:/var/lib/postgresql/data `
  postgres:17-alpine
```

Bei bereits vorhandenem, gestopptem Container:

```powershell
docker start werkmundo-issue34-document-db
```

Bereitschaft prüfen:

```powershell
docker exec werkmundo-issue34-document-db pg_isready -U document_user -d document-db
```

## 5. Document-Service mit PostgreSQL starten

Im PowerShell-Terminal innerhalb des lokalen Werkmundo-Repositories:

```powershell
Set-Location (Join-Path (git rev-parse --show-toplevel) "backend/services/document-service")

$dbPassword = Read-Host "Lokales Datenbankpasswort"

$env:DOCUMENT_STORAGE_POSTGRES_ENABLED="true"
$env:QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://127.0.0.1:55433/document-db"
$env:QUARKUS_DATASOURCE_USERNAME="document_user"
$env:QUARKUS_DATASOURCE_PASSWORD=$dbPassword
$env:QUARKUS_HTTP_PORT="8184"

mvn quarkus:dev
```

Die PostgreSQL-Anbindung und Flyway sind standardmäßig deaktiviert.
Mit `DOCUMENT_STORAGE_POSTGRES_ENABLED=true` werden sie aktiviert.

Flyway führt beim Start die vorhandenen Migrationen aus.

## 6. Tests ausführen

Den laufenden Dev-Modus zunächst mit Ctrl+C beenden.

Die Umgebungsvariablen aus Abschnitt 5 müssen im selben
PowerShell-Terminal weiterhin gesetzt sein.

```powershell
$env:QUARKUS_HTTP_TEST_PORT="8185"
mvn test
```

Die bestehenden API-Tests verwenden bei aktivierter
PostgreSQL-Konfiguration das PostgresDocumentRepository.

Die Authentifizierung wird in den Tests mit TestSecurity simuliert.
Die PDF-Erzeugung wird durch einen Testadapter ersetzt.

## 7. Datenbank überprüfen

Tabellen anzeigen:

```powershell
docker exec werkmundo-issue34-document-db psql -U document_user -d document-db -c "\dt"
```

Gespeicherte Dokumente zählen:

```powershell
docker exec werkmundo-issue34-document-db psql -U document_user -d document-db -c "SELECT document_type, COUNT(*) FROM documents GROUP BY document_type ORDER BY document_type;"
```

## 8. Verifizierte Ergebnisse (11.11.2026)

Lokale Testumgebung:

- PostgreSQL 17.11
- Quarkus 3.40.1.1
- Java 25
- Maven 3.9.12

Ergebnisse:

- PostgreSQL-Verbindung erfolgreich
- Flyway-Migration V1 erfolgreich angewendet
- Tabellen `documents` und `flyway_schema_history` vorhanden
- 7 Tests erfolgreich, 0 Fehler
- Maven BUILD SUCCESS
- 3 OFFER- und 1 INVOICE-Testdokument in PostgreSQL gespeichert

Die Datenbankintegration wurde lokal erfolgreich überprüft.
Die vollständige produktive Integration und die echte PDF-Erzeugung
wurden damit noch nicht nachgewiesen.

## 9. Hinweise

Die Testdatenbank ist unabhängig von der Offer-Service-Datenbank
aus Issue #32 und der bestehenden CraftVoice-Umgebung.

Passwörter dürfen nicht ins Repository committed werden.
Für andere Umgebungen müssen eigene Zugangsdaten verwendet werden.

Diese Dokumentation ersetzt weder Marcels Implementierung
noch die vorhandene Flyway-Migration.
