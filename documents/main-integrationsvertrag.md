# Integration in den aktuellen main-Stand

Geprüfte GitHub-Referenzen vom 10.11.2026: `main=2a52c57`,
`UI=60022fe`, `SquadAuftraggeber=2a5fa73`, `Arne=db7f483`, `Marcel=0a4f215`.
Repository: <https://github.com/thi-projekte/Werkmundo>.
Der offene PR #48 führt Arnes Branch nach `SquadAuftraggeber`.

## Zu erhaltende Funktionen

Die Integration darf die bestehenden User-Service-, Dashboard-, Sprach-,
Keycloak- und BPMN-Funktionen nicht durch den neuen CRUD-Service ersetzen.
Der neue Offer-Service deckt deren bisherige Schnittstellen noch nicht ab.
Die anderen Teamverzeichnisse wurden im Auftraggeber-Prüfbranch nicht geändert.

## Konkrete Abweichungen

| Aufrufer / bisheriger Vertrag | Neuer Auftraggeber-Stand | Konsequenz |
|---|---|---|
| UI: `POST /offers` mit `speechSnippet` und `customerId` | Erwartet `businessKey`, Positionen und weitere strukturierte Daten | Bestehende Erstellung mit Prozessstart erhalten; KI-Ergebnisse an die neue Anwendungsschicht anbinden |
| UI: `POST /offers/{key}/review/approve` | `POST /offers/{key}/approve` | Kompatibilitätsadapter und Vertragstest erforderlich |
| UI: `gesamtPreis`, `bezeichnung`, `positionsPreis`, `reihenfolge` | `totalPrice`, `name`, `lineTotal`, `positionOrder` | Explizites Mapping; UI nicht mit unbekannten Statuswerten pollen lassen |
| UI: `KI_FERTIG` und weitere Prozessstatus | `DRAFT`, `READY_TO_SEND` und weitere Angebotsstatus | Prozess- und Angebotsstatus fachlich zuordnen, nicht pauschal umbenennen |
| Dashboard-, Rechnungs-, Sprach- und PE-Endpunkte aus bisherigem Offer-Service | Im neuen Service nicht implementiert | Funktionen gezielt übernehmen und bisherige Tests weiter ausführen |
| UI erwartet Angebots-PDF-Download | Marcels Download akzeptiert nur Rechnungen | Angebots-PDF-Erzeugung und Download fehlen |
| Process Engine erzeugt Dokumente | Besitzer wird aus authentifiziertem JWT-`sub` bestimmt | Benutzer- und Servicekonto-Identität müssen abgestimmt werden; kein frei übergebbarer Besitzer ohne Berechtigungsprüfung |
| Patricks Offer-Docker-Build verwendet Java 21 | Neuer Service benötigt Java 25 | Build- und Runtime-Image gemeinsam anpassen |
| Patricks Stack setzt `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Neuer Offer-Service verwendet `OFFER_DB_*` bzw. Quarkus-Variablen | Startstack auf denselben Datenvertrag umstellen |
| Bestehende Tabellen der bisherigen Implementierung | Flyway-V1 mit eigener Tabellenstruktur | Bestehende Daten nicht ungeprüft migrieren; Migration gegen eine Datenkopie testen |

## Vorbereitete Verbesserungen

- Schutz gegen Überschreiben vorhandener Angebote und Regressionstests.
- Eigene PostgreSQL-Testdatenbank für Dokumente auf Port 55433, ohne Änderung
  des vorhandenen Handwerker-Stacks oder seiner Volumes.
- Vier Datenbanktests für Dokumente und getrennte Ausführung der API-Testadapter.
- Eigener Maven Wrapper und Java-25-Runtime-Dockerfile für Dokumente.
- OIDC-Erweiterung und externe Produktionskonfiguration für beide Services.
- Service-CI mit PostgreSQL und Prüfung, dass keine Datenbanktests übersprungen werden.
- Ein gemeinsames Windows-Prüfskript mit `clean verify` für beide Services.

## Nachweise

Vor den weiteren Änderungen wurden im lokalen Benutzerterminal bestätigt:
65 Offer-Tests einschließlich PostgreSQL und 7 Document-API-Tests, jeweils ohne
Fehler oder übersprungene Tests. Diese Ergebnisse bestätigen nicht die danach
ergänzten OIDC-Abhängigkeiten und vier neuen Dokumentdatenbanktests. Dafür muss
`scripts/Test-Auftraggeber.ps1` bzw. die neue CI erfolgreich ausgeführt werden.

Die isolierten Serviceprüfungen ersetzen keine Ende-zu-Ende-Prüfung von Browser,
Keycloak, beiden Services und CIB seven. Für `main` fehlt weiterhin die oben
beschriebene kompatible Zusammenführung der beiden Offer-Implementierungen.
Ein einfaches Auflösen der drei Git-Konflikte würde diese Arbeit nicht leisten.

Die neuen Dokumentdatenbank-Testquellen wurden mit Java 25 erfolgreich
kompiliert. Für diese isolierte Compilerprüfung wurde die noch nicht lokal
verfügbare OIDC-Abhängigkeit aus einer temporären POM herausgenommen. Das ist
ausdrücklich kein vollständiger Build des neuen Stands. PowerShell-Syntax und
Maven-XML wurden zusätzlich geprüft. Docker, HTTP-Start, OIDC-Laufzeit und die
neue CI müssen noch im Benutzerterminal bzw. auf GitHub ausgeführt werden.

## Referenzen

- Quarkus OIDC: <https://quarkus.io/guides/security-oidc-bearer-token-authentication/>
- Quarkus Flyway: <https://quarkus.io/guides/flyway/>
- Markus' Architektur: <https://winfprojekt.de/slides/architektur.html#1>
