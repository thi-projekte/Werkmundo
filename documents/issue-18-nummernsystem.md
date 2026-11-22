# Issue #18 - Fachliches Nummernsystem

## Ziel
Einführung fachlicher, lesbarer und eindeutiger Nummern für Angebote und Rechnungen. Der technische businessKey bleibt als interne Kennung erhalten.

## Festgelegte Nummernformate
- Angebote: `ANG-YYYYMM-NNNN`, Beispiel: `ANG-202611-0001`
- Rechnungen: `RE-YYYYMM-NNNN`, Beispiel: `RE-202611-0001`
- Angebote und Rechnungen erhalten getrennte Nummernkreise.
- Die laufende Nummer beginnt für jeden Kalendermonat bei 1.
- Für die Monatsbestimmung wird `Europe/Berlin` verwendet.
- Vergebene Nummern dürfen nicht geändert oder erneut vergeben werden.

## Implementierung: Angebotsnummer
Status: lokal implementiert und getestet.

- Service: `backend/services/offer-service`
- Flyway-Migration: `V3__add_offer_numbers.sql`
- Neue nullable Spalte `offer.angebotsnummer` mit eindeutigem Index.
- Tabelle `offer_number_counter` verwaltet die monatlichen Zähler.
- `OfferNumberGenerator` erzeugt die Nummern innerhalb einer Datenbanktransaktion.
- Die Nummer wird beim Speichern des Status `READY_TO_SEND` vergeben.
- Domain Model und JPA Repository speichern und lesen die Angebotsnummer.
- Die API liefert die Nummer als Feld `offerNumber` in `OfferResponse`.
- `PESSIMISTIC_WRITE` schützt das Speichern bestehender Angebote bei parallelen Zugriffen.
- Nummerierte Angebote können nicht physisch über `deleteByBusinessKey` gelöscht werden.
- Angebote ohne Nummer können weiterhin gelöscht werden.
- Die fachliche Stornierung bleibt von der Löschsperre getrennt.

## Tests
- Testumgebung: isolierte PostgreSQL-17-Datenbank für Issue #18, lokal auf Port 55434.
- Letzter vollständiger Testlauf: 173 Tests, 0 Fehler, 0 übersprungen, BUILD SUCCESS.
- Getestet: Nummernformat, unterschiedliche Nummern, Lesen und Speichern nach Commit.
- Getestet: paralleles Speichern desselben Angebots in einem Testszenario.
- Getestet: Schutz vor physischer Löschung nummerierter Angebote.
- Der erfolgreiche Parallelitätstest ersetzt keine umfassenden Last- und Wiederholungstests.

## Rechnungsnummer: Generator implementiert (Integration offen)
- Service: `backend/services/invoice-service`.
- Format: `RE-YYYYMM-NNNN`, mit eigenem monatlichem Zaehler.
- Flyway-Migration: `V4__add_invoice_number_counter.sql`.
- `InvoiceNumberGenerator` verwendet PostgreSQL und `Europe/Berlin`.
- Die Vergabe kann innerhalb derselben Datenbanktransaktion wie die Rechnungsausstellung erfolgen.
- Die bestehenden Rechnungsentwuerfe erhalten weiterhin keine Rechnungsnummer.
- Getestet: Format, fortlaufende Nummern und zwei parallele Anforderungen.
- Letzter vollstaendiger Testlauf im invoice-service: 52 Tests, 0 Fehler, 0 uebersprungen.
- Noch offen: Anbindung an die endgueltige Rechnungsausstellung und den PDF-Prozess.


## Offene Prüfungen
- Monatswechsel und Verhalten bei mehr als 9999 Nummern in einem Monat prüfen.
- Verhalten bei Wiederholungsanfragen und zusätzlichen Parallelitätsfällen prüfen.
- Integrationsablauf zur PDF-Erstellung und Anzeige im Frontend abstimmen.
- Integration der Rechnungsnummer in den finalen Ausstellungsprozess und zugehoerige Tests ergaenzen.

## Git und Zusammenarbeit
- Entwicklung in Worktree `Werkmundo-nummernsystem`.
- Feature-Branch: `Nahid/18_Fachliches_Nummernsystem`.
- Vorgesehener Ziel-Branch: `SquadAuftraggeber`.
- Änderungen erst nach gemeinsamer Prüfung committen und pushen.
