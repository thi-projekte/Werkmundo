# Document-Service

Java 25 / Quarkus 3.40.1.1. Der aktuelle Stand enthält REST-Endpunkte,
Besitzerprüfungen, ein PostgreSQL-Repository und PDF-Erzeugung für Rechnungen
und Auftragsbestätigungen. Ein Gateway oder eine Verbindung zum Offer-Service
ist noch nicht integriert.

## Datenbank

Die Migrationen unter `src/main/resources/db/migration/` legen die Tabelle
`documents` an und erlauben die Dokumenttypen Angebot, Rechnung und
Auftragsbestätigung. Die Tabelle enthält Besitzer, Dokumenttyp,
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

Alle Endpunkte erfordern eine authentifizierte Identität. Im lokalen
Quarkus-Dev-Modus ist Keycloak deaktiviert und eine lokale Basic-Auth-Identität
aktiviert:

| Benutzer | Passwort |
|---|---|
| `document-dev` | `document-dev-password` |

Diese Identität ist ausschließlich für `quarkus:dev` konfiguriert. Der
Dev-Server bindet an `127.0.0.1`; die eingebettete Benutzerverwaltung ist im
Produktionsprofil deaktiviert und dort bleibt OIDC erforderlich. Das Passwort
ist öffentlich und darf nicht für andere Umgebungen verwendet werden.

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/documents` | Metadaten eigener Dokumente |
| GET | `/documents/{documentId}` | Metadaten eines eigenen Dokuments |
| GET | `/documents/offers/{businessKey}` | Metadaten des neuesten eigenen Angebotsdokuments |
| GET | `/documents/{documentId}/pdf` | Eigenes Rechnungs- oder Auftragsbestätigungs-PDF herunterladen |
| POST | `/documents/invoices/{businessKey}/generate` | Rechnung aus einem Dokument-Snapshot erzeugen |
| POST | `/documents/order-confirmations/{businessKey}/generate` | Auftragsbestätigung aus einem Dokument-Snapshot erzeugen |
| POST | `/documents/offers/{businessKey}/share` | Angebotsdokument versenden |
| POST | `/documents/invoices/{businessKey}/share` | Rechnungsdokument versenden |

Beide Erzeugungsendpunkte verwenden denselben API-eigenen
`DocumentSnapshotRequest`-Vertrag. Er enthält Aussteller, Kunde, Positionen,
Währung, Nettobetrag, Gesamtbetrag und eine explizite `taxLines`-Liste. Der
Document-Service berechnet keine Steuern und leitet keine Beträge her: Die
übergebenen Werte werden dargestellt. Wenn noch keine Steueraufschlüsselung
vorliegt, wird `taxLines` als leere Liste übergeben. So kann die Steuerlogik
später ergänzt werden, ohne interne Offer-Service-Typen in die API aufzunehmen.

Beispiel für einen manuellen, authentifizierten Aufruf:

```powershell
$snapshot = @'
{
  "customerId": "customer-1",
  "documentDate": "2026-11-15",
  "issuer": { "name": "Werkmundo Handwerk", "address": "Werkstrasse 1" },
  "customer": {
    "name": "Max Mustermann",
    "address": "Kundenweg 2",
    "email": "kunde@example.com"
  },
  "items": [
    {
      "description": "Reparatur",
      "quantity": 2,
      "unit": "Stunden",
      "unitPrice": 50.00,
      "lineTotal": 100.00
    }
  ],
  "currency": "EUR",
  "netAmount": 100.00,
  "taxLines": [],
  "totalAmount": 100.00,
  "notes": "Termin nach Absprache"
}
'@

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/documents/invoices/invoice-123/generate" `
  -ContentType "application/json" -Headers @{ Authorization = "Bearer <token>" } `
  -Body $snapshot

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/documents/order-confirmations/order-123/generate" `
  -ContentType "application/json" -Headers @{ Authorization = "Bearer <token>" } `
  -Body $snapshot
```

Der `ownerId` wird nicht aus dem Snapshot angenommen, sondern aus der
authentifizierten Identität abgeleitet. Bis das Gateway verfügbar ist, können
die Endpunkte direkt mit Testdaten aufgerufen werden. Bei `quarkus:dev` kann
der oben genannte lokale Benutzer für Basic Auth verwendet werden. Der
Versandadapter ist weiterhin nicht produktiv implementiert.

## Noch fehlende Bausteine

- `DocumentSender`: Es existiert nur das Interface, keine produktive
  Versandimplementierung. Versand liefert ohne Implementierung `503`.
- Das Gateway und die automatische Übergabe vom Offer-Service stehen noch aus.
- Angebots-PDF-Erzeugung ist noch nicht umgesetzt.
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

Die API-Tests verwenden Testidentitäten, ein In-Memory-Repository, die echte
PDF-Erzeugung und einen simulierten Sender. Unit-Tests prüfen die Inhalte der
erzeugten PDFs einschließlich optionaler Steueraufschlüsselung. Die Tests
bestätigen keine E-Mail-Zustellung; PostgreSQL wird in den separaten
opt-in-Tests geprüft.

Fünf zusätzliche PostgreSQL-Tests prüfen binäre Inhalte, Besitzertrennung,
Dokumentversionen, den neuen Dokumenttyp und die Flyway-Migrationen. Ohne
`DOCUMENT_DB_TESTS=true` werden ausschließlich diese fünf Tests übersprungen. Für den vollständigen
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
