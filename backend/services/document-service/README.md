# Document-Service

Java 25 / Quarkus 3.40.1.1. Der aktuelle Stand enthält REST-Endpunkte,
Besitzerprüfungen, ein PostgreSQL-Repository und eine Flyway-Migration für
Dokumentmetadaten und PDF-Inhalte. Er ist noch kein vollständig integrierter
PDF- und Versandservice.

## Datenbank

Die Migration `src/main/resources/db/migration/V1__create_documents.sql`
legt die Tabelle `documents` an. Sie enthält Besitzer, Dokumenttyp,
Referenz, Empfänger, Dateiname, PDF-Bytes (`bytea`) und Erstellungszeitpunkt.
Abfragen sind nach Besitzer getrennt. SQL-Parameter werden gebunden.

`docker-compose.db.yml` stellt eine eigene PostgreSQL-17-Datenbank auf
`127.0.0.1:55433` bereit: Datenbank `document_db`, Benutzer `document_user`.
Vor dem Start `DOCUMENT_DB_PASSWORD` als Umgebungsvariable setzen, dann
`docker compose -f docker-compose.db.yml up -d --wait` ausführen.
Die Datenbank des Offer-Service wird nicht mitverwendet. Das benannte Volume
bleibt bei `docker compose -f docker-compose.db.yml down` erhalten.

Für eine separat bereitgestellte PostgreSQL-Datenbank konfigurieren:

| Umgebungsvariable | Zweck |
|---|---|
| `DOCUMENT_STORAGE_POSTGRES_ENABLED=true` | Repository, Datenquelle und Migration aktivieren |
| `QUARKUS_DATASOURCE_JDBC_URL` | JDBC-URL der eigenen Dokumentdatenbank |
| `QUARKUS_DATASOURCE_USERNAME` | Datenbankbenutzer |
| `QUARKUS_DATASOURCE_PASSWORD` | Kennwort zur Laufzeit |

Ohne aktivierte PostgreSQL-Speicherung gibt es außerhalb der Tests keine
alternative Speicherimplementierung. Speicherendpunkte antworten dann mit
`503 Service Unavailable`.

## Schnittstellen

Alle Endpunkte erfordern eine authentifizierte Identität.

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/documents` | Metadaten eigener Dokumente |
| GET | `/documents/{documentId}` | Metadaten eines eigenen Dokuments |
| GET | `/documents/offers/{businessKey}` | Metadaten des neuesten eigenen Angebotsdokuments |
| GET | `/documents/{documentId}/pdf` | PDF eines Rechnungsdokuments; Angebote werden derzeit abgewiesen |
| POST | `/documents/invoices/{businessKey}/generate` | Rechnungsdokument aus `rechnungsentwurf` erzeugen |
| POST | `/documents/offers/{businessKey}/share` | Angebotsdokument versenden |
| POST | `/documents/invoices/{businessKey}/share` | Rechnungsdokument versenden |

Der Rechnungsentwurf wird als JSON-Objekt mit
`rechnungsentwurf.kundendaten.email` erwartet. Erzeugung und Versand brauchen
die unten genannten konkreten Adapter.

## Noch fehlende Bausteine

- `InvoicePdfGenerator`: Es existiert nur das Interface, keine produktive
  PDF-Erzeugung. Der Erzeugungsendpunkt liefert ohne Implementierung `503`.
- `DocumentSender`: Es existiert nur das Interface, keine produktive
  Versandimplementierung. Versand liefert ohne Implementierung `503`.
- Erzeugung von Angebots-PDFs und deren Download sind noch nicht umgesetzt.
- Die produktive OIDC-Konfiguration ist vorbereitet: `KEYCLOAK_AUTH_SERVER_URL`
  und bei Bedarf `KEYCLOAK_CLIENT_ID` setzen. Der Besitzer wird aus dem JWT-Claim
  `sub` abgeleitet. Der gemeinsame Test mit echten Keycloak-Tokens steht aus.
- Maven Wrapper und ein Java-25-Runtime-Dockerfile unter `src/main/docker/`
  sind vorhanden. Das Runtime-Image wird nach `./mvnw package` gebaut:
  `docker build -f src/main/docker/Dockerfile.jvm -t werkmundo/document-service:local .`.
  Die Integration in den gemeinsamen Startstack steht noch aus.

## Tests

Mit Java 25 im Serviceverzeichnis:

```powershell
.\mvnw.cmd -B '-Dquarkus.http.test-port=18082' test
```

Die vorhandenen sieben API-Tests verwenden Testidentitäten, ein
In-Memory-Repository, drei feste Bytes als PDF und einen simulierten Sender.
Sie prüfen API-Verhalten und Besitzertrennung. Sie bestätigen weder echte
PDF-Erzeugung oder E-Mail-Zustellung noch das PostgreSQL-Repository.

Vier zusätzliche PostgreSQL-Tests prüfen binäre Inhalte, Besitzertrennung,
Dokumentversionen und die Flyway-Migration. Ohne `DOCUMENT_DB_TESTS=true`
werden ausschließlich diese vier Tests übersprungen. Für den vollständigen
Lauf mit laufender Dokumentdatenbank:

```powershell
$env:DOCUMENT_DB_TESTS = 'true'
$env:QUARKUS_DATASOURCE_JDBC_URL = 'jdbc:postgresql://127.0.0.1:55433/document_db'
$env:QUARKUS_DATASOURCE_USERNAME = 'document_user'
$env:QUARKUS_DATASOURCE_PASSWORD = $env:DOCUMENT_DB_PASSWORD
.\mvnw.cmd -B '-Dquarkus.http.test-port=18082' clean verify
```

Die Tests verwenden eindeutig erzeugte Testbesitzer und entfernen nur ihre
eigenen Testdokumente. Ein automatischer Gesamtlauf beider Services steht im
Repository-Root unter `scripts/Test-Auftraggeber.ps1`. Die GitHub Action
`auftraggeber-tests.yml` führt beide Services gegen PostgreSQL aus und weist
übersprungene Datenbanktests zurück.
