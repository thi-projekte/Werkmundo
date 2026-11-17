# Document-Service

Java 25 / Quarkus 3.40.1.1. Der aktuelle Stand enthält REST-Endpunkte,
Besitzerprüfungen, ein PostgreSQL-Repository sowie PDF-Erzeugung und
E-Mail-Versand für Angebote, Rechnungen und Auftragsbestätigungen. Mit Mailpit
können lokal abgefangene E-Mails samt Anhang im Browser geprüft werden. Ein
Gateway oder eine Verbindung zum Offer-Service ist noch nicht integriert.

## Datenbank

Die Migrationen unter `src/main/resources/db/migration/` legen die Tabelle
`documents` an und erlauben die Dokumenttypen Angebot, Rechnung und
Auftragsbestätigung. Die Tabelle enthält Besitzer, Dokumenttyp,
Referenz, Empfänger, Dateiname, PDF-Bytes (`bytea`) und Erstellungszeitpunkt.
Abfragen sind nach Besitzer getrennt. SQL-Parameter werden gebunden.

`docker-compose.db.yml` stellt eine eigene PostgreSQL-17-Datenbank auf
`127.0.0.1:55433` bereit: Datenbank `document_db`, Benutzer `document_user`.
Das lokale Passwort gehört in die nicht versionierte Datei `.env` im
Document-Service-Verzeichnis:

```dotenv
DOCUMENT_DB_PASSWORD=hier-das-passwort-eintragen
```

Bei einer neuen Datenbank kannst du selbst ein Passwort festlegen. Bei einem
bereits vorhandenen Datenbank-Volume muss hier genau das Passwort stehen, mit
dem dieses Volume ursprünglich initialisiert wurde. Ein anderer Wert ändert
das gespeicherte PostgreSQL-Passwort nicht und führt zu
`password authentication failed`. Die Datei `.env` ist durch `.gitignore` vom
Commit ausgeschlossen.

Die Compose-Datei verwendet `.env` automatisch. Den Document-Service startest
du in einer PowerShell ebenfalls mit diesem Wert; die konkreten Befehle stehen
im Mailpit-Ablauf weiter unten.
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

## E-Mail-Versand

Der Versand verwendet Quarkus Mailer und hängt das gespeicherte PDF an. In
Produktion SMTP-Zugangsdaten als Umgebungsvariablen bereitstellen:

| Umgebungsvariable | Zweck |
|---|---|
| `DOCUMENT_MAIL_FROM` | Absenderadresse |
| `SMTP_HOST` | SMTP-Server |
| `SMTP_PORT` | SMTP-Port, Standard `587` |
| `SMTP_USERNAME` | Optionaler SMTP-Benutzer |
| `SMTP_PASSWORD` | Optionales SMTP-Passwort |

Im Entwicklungs- und Testprofil wird der Mailversand simuliert. Angebote
benötigen beim Versand einen vollständigen HTTP(S)-Link (`accessLink`), den der
Aufrufer bereitstellt. Rechnungen und Auftragsbestätigungen werden ohne Link
versendet. Der Document-Service erzeugt oder speichert den Angebotstoken nicht.

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
| POST | `/documents/offers/{businessKey}/generate` | Angebot aus einem Dokument-Snapshot erzeugen |
| POST | `/documents/invoices/{businessKey}/generate` | Rechnung aus einem Dokument-Snapshot erzeugen |
| POST | `/documents/order-confirmations/{businessKey}/generate` | Auftragsbestätigung aus einem Dokument-Snapshot erzeugen |
| POST | `/documents/offers/{businessKey}/share` | Angebotsdokument versenden |
| POST | `/documents/invoices/{businessKey}/share` | Rechnungsdokument versenden |
| POST | `/documents/order-confirmations/{businessKey}/share` | Auftragsbestätigung versenden |

Alle Erzeugungsendpunkte verwenden denselben API-eigenen
`DocumentSnapshotRequest`-Vertrag. Er enthält Aussteller, Kunde, Positionen,
Währung, Nettobetrag, Gesamtbetrag und eine explizite `taxLines`-Liste. Der
Document-Service berechnet keine Steuern und leitet keine Beträge her: Die
übergebenen Werte werden dargestellt. Wenn noch keine Steueraufschlüsselung
vorliegt, wird `taxLines` als leere Liste übergeben. So kann die Steuerlogik
später ergänzt werden, ohne interne Offer-Service-Typen in die API aufzunehmen.
Zum Teilen eines Angebots wird eine vollständige URL mitgeschickt, zum Beispiel:

```powershell
$shareRequest = @{ accessLink = "https://example.test/offer?token=opaque-token" } |
  ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/documents/offers/angebot-123/share" `
  -ContentType "application/json" -Headers @{ Authorization = "******" } `
  -Body $shareRequest
```

Der Link wird als opake Zeichenkette validiert und in den E-Mail-Text eingesetzt;
der Document-Service interpretiert oder speichert den Token nicht. Für einen
Angebotsversand sind sowohl eine Empfänger-E-Mail-Adresse als auch ein
HTTP(S)-Link erforderlich.

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
der oben genannte lokale Benutzer für Basic Auth verwendet werden.

Nach dem Generieren werden die Dokumente über diese Endpunkte versendet:

```powershell
# Angebot: accessLink muss mitgeschickt werden.
$shareRequest = @{ accessLink = "https://example.test/offer?token=opaque-token" } |
  ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/documents/offers/angebot-123/share" `
  -ContentType "application/json" -Headers @{ Authorization = "******" } `
  -Body $shareRequest

# Rechnung und Auftragsbestätigung: kein Request-Body und kein Link.
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/documents/invoices/invoice-123/share" `
  -Headers @{ Authorization = "******" }
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/documents/order-confirmations/order-123/share" `
  -Headers @{ Authorization = "******" }
```

## Vollständiger lokaler Mailpit-Test

Mailpit nimmt die SMTP-Nachrichten lokal entgegen und zeigt Nachrichtentext,
Empfänger und PDF-Anhänge in einer Weboberfläche an. Die E-Mails werden dabei
nicht an echte Empfänger zugestellt. Voraussetzung ist Docker.

Öffne drei PowerShell-Fenster und lasse die beiden Container sowie den
Document-Service während des Tests laufen.

**Fenster 1 – Mailpit starten:**

```powershell
docker run --rm --name mailpit `
  -p 127.0.0.1:1025:1025 `
  -p 127.0.0.1:8025:8025 `
  axllent/mailpit
```

Mailpit nimmt SMTP auf `127.0.0.1:1025` an. Die Mailbox wird unter
<http://127.0.0.1:8025> angezeigt.

**Fenster 2 – Dokumentdatenbank starten:**

```powershell
Set-Location "backend\services\document-service"
$env:DOCUMENT_DB_PASSWORD = (Get-Content .env | ConvertFrom-StringData).DOCUMENT_DB_PASSWORD
docker compose -f docker-compose.db.yml up -d --wait
```

Verwende in `.env` bei einem vorhandenen Volume das Passwort, das bei dessen
erstmaliger Initialisierung verwendet wurde. Du musst das Passwort nicht in
den Befehlen wiederholen oder ändern.

**Fenster 3 – Document-Service mit Mailpit starten:**

```powershell
Set-Location "backend\services\document-service"
$env:DOCUMENT_DB_PASSWORD = (Get-Content .env | ConvertFrom-StringData).DOCUMENT_DB_PASSWORD
$env:DOCUMENT_STORAGE_POSTGRES_ENABLED = "true"
$env:QUARKUS_DATASOURCE_JDBC_URL = "jdbc:postgresql://127.0.0.1:55433/document_db"
$env:QUARKUS_DATASOURCE_USERNAME = "document_user"
$env:QUARKUS_DATASOURCE_PASSWORD = $env:DOCUMENT_DB_PASSWORD

# Überschreibt den Dev-Mail-Mock: Mails gehen an Mailpit, nicht ins Internet.
$env:QUARKUS_MAILER_MOCK = "false"
$env:QUARKUS_MAILER_HOST = "127.0.0.1"
$env:QUARKUS_MAILER_PORT = "1025"
$env:QUARKUS_MAILER_START_TLS = "DISABLED"
$env:DOCUMENT_MAIL_FROM = "werkmundo@example.test"

.\mvnw.cmd quarkus:dev
```

Verwende das gleiche Datenbankpasswort wie in Fenster 2. Warte, bis Quarkus
gestartet ist. Erzeuge und versende danach die drei Dokumenttypen aus einem
vierten PowerShell-Fenster:

```powershell
$base = "http://localhost:8080"
$pair = "document-dev:document-dev-password"
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{ Authorization = "Basic $basic" }
$runId = Get-Date -Format "yyyyMMddHHmmss"

$snapshot = @'
{
  "customerId": "mailpit-customer",
  "documentDate": "2026-11-17",
  "issuer": {
    "name": "Werkmundo Handwerk",
    "address": "Werkstrasse 1, 85049 Ingolstadt"
  },
  "customer": {
    "name": "Max Mustermann",
    "address": "Kundenweg 2, 85051 Ingolstadt",
    "email": "test-inbox@example.test"
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
  "notes": "Mailpit-Versandtest"
}
'@

# Angebot erzeugen und mit einem Test-Link versenden.
$offerKey = "mailpit-offer-$runId"
$offer = Invoke-RestMethod -Method Post `
  -Uri "$base/documents/offers/$offerKey/generate" `
  -ContentType "application/json" -Headers $headers -Body $snapshot
$offerShare = @{
  accessLink = "https://example.test/offer?token=mailpit-test"
} | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri "$base/documents/offers/$offerKey/share" `
  -ContentType "application/json" -Headers $headers -Body $offerShare

# Rechnung erzeugen und ohne Link versenden.
$invoiceKey = "mailpit-invoice-$runId"
$invoice = Invoke-RestMethod -Method Post `
  -Uri "$base/documents/invoices/$invoiceKey/generate" `
  -ContentType "application/json" -Headers $headers -Body $snapshot
Invoke-RestMethod -Method Post `
  -Uri "$base/documents/invoices/$invoiceKey/share" -Headers $headers

# Auftragsbestätigung erzeugen und ohne Link versenden.
$orderKey = "mailpit-order-$runId"
$order = Invoke-RestMethod -Method Post `
  -Uri "$base/documents/order-confirmations/$orderKey/generate" `
  -ContentType "application/json" -Headers $headers -Body $snapshot
Invoke-RestMethod -Method Post `
  -Uri "$base/documents/order-confirmations/$orderKey/share" -Headers $headers

# Die gespeicherten PDFs zusätzlich lokal herunterladen.
Invoke-WebRequest -Method Get -Uri "$base/documents/$($offer.documentId)/pdf" `
  -Headers $headers -OutFile ".\angebot-mailpit.pdf"
Invoke-WebRequest -Method Get -Uri "$base/documents/$($invoice.documentId)/pdf" `
  -Headers $headers -OutFile ".\rechnung-mailpit.pdf"
Invoke-WebRequest -Method Get -Uri "$base/documents/$($order.documentId)/pdf" `
  -Headers $headers -OutFile ".\auftragsbestaetigung-mailpit.pdf"
```

Öffne <http://127.0.0.1:8025>. Dort sollten drei E-Mails an
`test-inbox@example.test` erscheinen. Prüfe Betreff und Nachrichtentext und
öffne jeweils den PDF-Anhang. Nur die Angebotsmail enthält den Test-Link.
Die drei heruntergeladenen PDFs liegen im aktuellen Verzeichnis des vierten
PowerShell-Fensters. Beende Mailpit und den Service nach dem Test mit
`Ctrl+C`; der Mailpit-Container wird durch `--rm` beim Beenden entfernt.

## Noch fehlende Bausteine

- Das Gateway und die automatische Übergabe vom Offer-Service stehen noch aus.
- Die Token-Erzeugung und das endgültige Angebotslink-Format werden vom
  aufrufenden Gateway/Offer-Service festgelegt und sind noch nicht angebunden.
- Die produktive OIDC-Konfiguration ist vorbereitet: `KEYCLOAK_AUTH_SERVER_URL`
  und bei Bedarf `KEYCLOAK_CLIENT_ID` setzen. Der Besitzer wird aus dem JWT-Claim
  `sub` abgeleitet. Der gemeinsame Test mit echten Keycloak-Tokens steht aus.
- Maven Wrapper und ein Java-25-Runtime-Dockerfile unter `src/main/docker/`
  sind vorhanden. Das Runtime-Image wird nach `./mvnw package` gebaut:
  `docker build -f src/main/docker/Dockerfile.jvm -t werkmundo/document-service:local .`.
  Die Integration in den gemeinsamen Startstack steht noch aus.

## Tests

Im Serviceverzeichnis mit Java 25 ausführen:

```powershell
.\mvnw.cmd -B test
```

Alle Tests sind Unit-Tests ohne gestartete Quarkus-Anwendung, HTTP-Server oder
PostgreSQL-Datenbank. `DocumentResourceTest` prüft Erzeugung und Versand für
Angebote, Rechnungen und Auftragsbestätigungen mit einem In-Memory-Repository
und simuliertem Sender. `PdfBoxDocumentPdfGeneratorTest` prüft den Inhalt der
PDFs. SMTP-Zustellung und PostgreSQL-Persistenz werden in diesem Testlauf nicht
geprüft.
