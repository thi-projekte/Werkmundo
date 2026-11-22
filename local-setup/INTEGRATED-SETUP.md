# Lokales Gesamt-Setup

## Voraussetzungen und Start

Docker Desktop mit Linux-Containern und Docker Compose ab Version 2.24.4.

Im Repository-Hauptordner:

```powershell
.\Start-Local.cmd
```

Das Skript baut die Images und startet Frontend, API-Gateway, User-, Offer-,
Invoice- und Catalog-Service sowie den optionalen AI-Service.

Beim ersten Start wird `local-setup/.env.local` mit lokalen Passwörtern erzeugt.
Diese Datei wird nicht versioniert. Bestehende Passwörter für weitere Starts
beibehalten. Bei älteren Konfigurationen muss `CATALOG_DB_PASSWORD` ergänzt sein.

Weitere Startoptionen:

```powershell
.\Start-Local.cmd -ValidateOnly
.\Start-Local.cmd -WithoutAi
```

## Zugriff

Frontend: http://localhost:5173

Backend-Aufrufe des Docker-Frontends laufen über Nginx und das API-Gateway:

| Präfix | Ziel |
|---|---|
| `/svc/user/` | User-Service |
| `/svc/offer/` | Offer-Service |
| `/svc/invoice/` | Invoice-Service |
| `/svc/catalog/` | Catalog-Service |

Das Gateway prüft Login-Tokens und leitet sie weiter.
Die Services behalten ihre fachlichen Berechtigungsprüfungen.

Registrierung, Passwort-Zurücksetzung und definierte Kundenlink-Endpunkte
sind ohne Benutzerlogin erreichbar. Angebotstokens werden im Offer-Service geprüft.

Der lokale Browserlogin verwendet den gemeinsamen Realm:
https://terramax-keycloak.winfprojekt.de/realms/werkmundo

Der zusätzlich gestartete lokale Keycloak auf Port 8090 wird für die
bestehende Admin-Anbindung des User-Service verwendet.

## Lokale Diagnoseports

Gateway: 8087; User: 8081; Offer: 8082; Catalog: 8083;
Invoice: 8084; optionaler AI-Service: 8086.

Die Backend-Ports sind weiterhin direkt über localhost erreichbar.
User, Offer, Invoice und Catalog verwenden eigene Datenbankcontainer und Volumes.
Die separaten Testdatenbanken auf 55432 und 55433 bleiben davon unabhängig.

## Gateway-Tests

Mit lokal installiertem JDK 25:

```powershell
.\backend\services\offer-service\mvnw.cmd -B -f .\gateway\pom.xml clean test
```

Die Tests verwenden eigene HTTP-Testserver und simulierte Benutzeridentitäten.
Docker und ein erreichbarer Keycloak sind dafür nicht erforderlich.
Die kryptografische JWT-Prüfung wird durch diese Tests nicht abgedeckt.

## Grenzen des aktuellen Setups

Die Process Engine wird nicht mitgestartet. Vollständige Prozessabläufe benötigen
eine erreichbare Engine mit passenden Prozessen und Delegates.

Der AI-Service verwendet ohne Provider-Key seinen Ersatzbetrieb.
Katalogdaten werden nicht automatisch importiert.

Die Profilseite speichert ihre Einstellungen derzeit im Browser-sessionStorage.
Gültige Kundenlinks, Uploads, PDF-Downloads und die vollständige Rechnungserstellung
sind noch gesondert über das Gateway zu prüfen.

Die Produktionskonfiguration wurde durch die lokale Gateway-Anbindung
noch nicht umgestellt.

## Stoppen

Ohne Löschen der gespeicherten Daten:

```powershell
docker compose --env-file local-setup/.env.local -f local-setup/docker-compose.local.yml -f local-setup/docker-compose.integrated.yml --profile ai stop
```

`down -v` löscht die zugehörigen Volumes und darf nicht verwendet werden,
wenn die Daten erhalten bleiben sollen.