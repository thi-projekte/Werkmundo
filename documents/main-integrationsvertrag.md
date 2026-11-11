# Lokaler Auftraggeber-Integrationsstand

Stand: 11.11.2026. Der Arbeitsbranch basiert auf `SquadAuftraggeber` und
enthält den eingelesenen `main`-Stand `2a52c575d7a74bdb17b9154c5ff36650e38f95c8`.

## Ergebnis

- Ein Offer-Maven-Modul; kein dauerhafter zweiter Offer-Service.
- Bestehende UI-/Prozessverträge bleiben erhalten. Die strukturierte
  Auftraggeber-API verwendet `/api/offers`.
- Im PostgreSQL-Betrieb verwenden beide APIs denselben Angebotsbestand.
- Dashboard-, Rechnungs-, Sprach- und Prozessklassen bleiben enthalten.
- Der Document-Service verwendet weiterhin seine eigene PostgreSQL-Datenbank.
- V1 ist unverändert. V2 konvertiert nur leere Squad-Ausgangstabellen und
  verweigert eine Übernahme vorhandener Daten. Eine Servermigration ist damit
  ausdrücklich nicht umgesetzt.

## Lokaler Nachweis

`scripts/Test-Auftraggeber.ps1`: 169 Offer-Tests und 11 Document-Tests am
11.11.2026 erfolgreich, keine Fehler oder übersprungenen Tests, beide Builds
erfolgreich. Offer-Regressionen laufen isoliert mit H2; die expliziten
Persistenztests beider Services verwenden PostgreSQL.

## Grenzen vor einem Merge nach main

Der gemeinsame Docker-Start wurde noch nicht angepasst und nachgewiesen.
Patricks ursprünglicher Offer-Build verwendet Java 21; das gemeinsame Modul
benötigt Java 25. Die bisherigen DB_HOST-/DB_NAME-Variablen müssen mit den
OFFER_DB_*- und Document-Service-Einstellungen abgestimmt werden.

Browser, echte Keycloak-Tokens, technische Servicekonten und BPMN-Gesamtablauf
sind nicht vollständig getestet. Die übernommene Process-Engine-Integration
bleibt erhalten, ihre tatsächliche Funktion ist dadurch nicht nachgewiesen.

PDF-Erzeugung, E-Mail-Versand, Angebotsnummern, Dokumentrevisionen und
Auftragsbestätigung bleiben eigene Arbeitspakete. Der getestete Zwischenstand
ist keine Freigabe für eine Serverbereitstellung.

Service-Details: [Offer-Service](../backend/services/offer-service/README.md)
und [Document-Service](../backend/services/document-service/README.md).
