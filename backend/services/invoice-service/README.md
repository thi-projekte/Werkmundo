# Invoice-Service

Java 25 / Quarkus 3.40.1.1. Der aktuelle Stand enthält REST-Endpunkte,
Besitzerprüfungen, ein PostgreSQL-Repository und eine Flyway-Migration für
Dokumentmetadaten und PDF-Inhalte. Rechnungs-PDFs werden mit PDFBox erzeugt;
der Versandadapter ist noch nicht integriert.

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
| GET | `/documents/{documentId}/pdf` | PDF als Download |
| GET | `/documents/{documentId}/pdf?inline=true` | PDF für die Browser-Vorschau |
| POST | `/documents/invoices/{businessKey}/generate` | Rechnungsdokument aus `rechnungsentwurf` erzeugen |
| POST | `/documents/offers/{businessKey}/generate` | Angebotsdokument aus `snapshot` erzeugen |
| POST | `/documents/offers/{businessKey}/preview` | Nicht gespeicherte Angebots-PDF-Vorschau erzeugen |
| POST | `/documents/offers/{businessKey}/share` | Angebotsdokument versenden |
| POST | `/documents/invoices/{businessKey}/share` | Rechnungsdokument versenden |

Der Rechnungsentwurf enthält unter `rechnungsentwurf` die Felder
`rechnungsnummer`, `createdAt`, `gesamtPreis`, `kundendaten` und `positions`.
Die Nummer wird im PDF als Platzhalter gekennzeichnet. `issuer` kann optionale
Absenderdaten enthalten. Für den ersten Entwurf wird `gesamtPreis` als Netto-
und Gesamtbetrag verwendet; die Währung ist EUR.

Angebots- und Rechnungs-PDFs verwenden dasselbe Layout mit Werkmundo-Logo und
Markenfarben. Im Rechnungs-PDF wird die Nummer weiterhin als Platzhalter markiert.
Angezeigt werden die Absenderfelder, die im Profil vorhanden sind.

Die Angebotsvorschau speichert das PDF nicht. Sie kann auch ohne Positionen
erstellt werden; ein leeres PDF wird nicht als versendbares Dokument gespeichert.

## Noch fehlende Bausteine

- `DocumentSender`: Es existiert nur das Interface, keine produktive
  Versandimplementierung. Versand liefert ohne Implementierung `503`.
- Die produktive OIDC-Konfiguration ist vorbereitet: `KEYCLOAK_AUTH_SERVER_URL`
  und bei Bedarf `KEYCLOAK_CLIENT_ID` setzen. Der Besitzer wird aus dem JWT-Claim
  `sub` abgeleitet. Der gemeinsame Test mit echten Keycloak-Tokens steht aus.
- Maven Wrapper und ein Java-25-Runtime-Dockerfile unter `src/main/docker/`
  sind vorhanden. Das Runtime-Image wird nach `./mvnw package` gebaut:
  `docker build -f src/main/docker/Dockerfile.jvm -t werkmundo/invoice-service:local .`.
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
