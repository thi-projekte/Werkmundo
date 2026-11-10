# Offer-Service

Java 25 / Quarkus 3.40.1.1. Der Service verwaltet Angebote, Positionen,
Statushistorien, Korrekturhinweise und Kundenänderungswünsche. Die produktive
Persistenz verwendet PostgreSQL mit Flyway; lokal ist alternativ ein
In-Memory-Repository verfügbar.

## Einrichtung

Die vollständigen PowerShell-Schritte, Datenbankvariablen und CRUD-Beispiele
stehen in [SERVICE_AND_DATABASE_SETUP.md](SERVICE_AND_DATABASE_SETUP.md).
Die dortigen persönlichen Verzeichnispfade durch den eigenen Checkout ersetzen.

- Java 25 und Docker Desktop sind für den Datenbanktest erforderlich.
- `docker-compose.db.yml` startet PostgreSQL 17 auf `127.0.0.1:55432`.
- Datenbank: `offer_db`, Benutzer: `offer_user`.
- Das Kennwort wird über `OFFER_DB_PASSWORD` gesetzt.
- `OFFER_PERSISTENCE=postgres` und `OFFER_DB_ENABLED=true` aktivieren die
  Datenbank für lokale Entwicklungs- und Testläufe.
- Die Migration legt fünf Tabellen an. Die Daten bleiben im benannten Volume.
- `docker compose -f docker-compose.db.yml down` stoppt die Datenbank, ohne
  das Volume zu löschen.

## Schnittstellen

| Methode | Pfad | Zweck |
|---|---|---|
| POST | `/offers` | Angebot mit neuem `businessKey` anlegen |
| GET | `/offers` | Eigene Angebote auflisten |
| GET / PUT / DELETE | `/offers/{businessKey}` | Eigenes Angebot lesen, ändern oder löschen |
| POST | `/offers/{businessKey}/approve` | Freigeben und Kundenzugriffstoken ausstellen |
| POST | `/offers/{businessKey}/sent` | Versandstatus setzen |
| POST | `/offers/{businessKey}/cancel` | Angebot stornieren |
| GET | `/offers/{businessKey}/customer` | Angebot mit Kundenzugriffstoken lesen |
| POST | `/offers/{businessKey}/customer/accept` | Angebot annehmen |
| POST | `/offers/{businessKey}/customer/reject` | Angebot ablehnen |
| POST | `/offers/{businessKey}/customer/changes` | Änderungswunsch übermitteln |

Handwerkeraktionen benötigen einen authentifizierten Principal und prüfen den
Besitzer. Für lokale manuelle Tests ist Basic Auth ausschließlich im
Entwicklungsprofil konfiguriert. Kunden verwenden den Header
`X-Offer-Access-Token`. Gespeichert werden Token-Hash und Ablaufzeit, nicht das
Zugriffstoken selbst. Ein bereits vorhandener `businessKey` darf beim Anlegen
nicht überschrieben werden.

## Prüfungen

Unit-Tests ohne laufenden HTTP-Server und ohne Datenbank:

```powershell
.\mvnw.cmd -B '-Dtest=*,!HealthResourceTest,!OfferEntityRepositoryTest,!OfferJpaRepositoryIntegrationTest' test
```

Vollständiger Testlauf mit laufender PostgreSQL-Datenbank und den oben
gesetzten Datenbankvariablen:

```powershell
.\mvnw.cmd -B '-Dquarkus.http.test-port=18081' test
```

## Offene Integration

Diese Implementierung unterscheidet sich von der bestehenden Implementierung
unter `de.winfprojekt.craftvoice.offerservice` auf `main`. Beide dürfen nicht
unverändert in dieselbe Anwendung übernommen werden: REST-Routen und
Datenbanktabellen überschneiden sich, Konfiguration und Datenverträge weichen ab.

- Keycloak/OIDC ist in diesem Service-Branch noch nicht eingerichtet.
- KI-, Katalog-, User-Service- und Workflow-Adapter enthalten noch Platzhalter.
- Die UI erwartet teilweise andere Endpunkte, Feldnamen und Statuswerte.
- Patricks Docker-Build aus `main` verwendet Java 21 und muss für diese
  Implementierung auf Java 25 abgestimmt werden.
- Erfolgreiche Unit-Tests ersetzen keine HTTP-, PostgreSQL- oder Gesamtsystemtests.

Der dokumentierte frühere Datenbanktest bestätigt nicht automatisch den
aktuellen vollständigen Integrationsstand.
