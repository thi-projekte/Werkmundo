# Offer-Service: lokaler Prüfstand

Voraussetzungen: Java 25, Docker Desktop und eine PowerShell im Repository-Root.

## Vollständiger vorhandener Service-Prüflauf

```powershell
.\scripts\Test-Auftraggeber.ps1
```

Der Lauf startet die Offer- und Document-PostgreSQL-Datenbanken, führt Builds
und Tests aus und weist übersprungene Tests zurück. Echte Zugangsdaten nicht
in Quellcode oder Logausgaben übernehmen.

| Service | Host-Port | Datenbank | Benutzer | Kennwortvariable |
|---|---|---|---|---|
| Offer | 55432 | `offer_db` | `offer_user` | `OFFER_DB_PASSWORD` |
| Document | 55433 | `document_db` | `document_user` | `DOCUMENT_DB_PASSWORD` |

Die Compose-Dateien `docker-compose.db.yml` in den Serviceverzeichnissen
verwenden getrennte Container und Volumes. Ein normales `down` erhält die
Volumes. Zum erneuten Start müssen die bisherigen Kennwörter verwendet werden.

Für die gemeinsame Offer-Persistenz sind `OFFER_PERSISTENCE=postgres`,
`OFFER_DB_ENABLED=true`, `OFFER_DB_JDBC_URL`, `OFFER_DB_USER` und
`OFFER_DB_PASSWORD` maßgeblich. Das Gesamtskript setzt die Testvariablen für
seine Laufzeit und stellt anschließend die vorherige Umgebung wieder her.

## Migrationsgrenze

V2 ist nur für leere Squad-Ausgangstabellen vorgesehen und bricht bei Daten
ab. Gefüllte Datenbanken weder löschen noch durch Zurücksetzen von Flyway
umgehen. Eine vorhandene Serverdatenbank benötigt eine gesondert geprüfte
Übernahme. Angewendete Migrationen unverändert lassen.

## Lokaler Gesamtstart

Das gemeinsame Startsetup wird in einem separaten Schritt angepasst.
Patricks bisheriges Setup allein bestätigt noch keinen Start dieses
zusammengeführten Stands. Insbesondere Java 25 und die Variablen beider
neuen Datenbanken müssen darin berücksichtigt werden.

Aktuelle API-Pfade und Funktionsgrenzen stehen in [README.md](README.md).
