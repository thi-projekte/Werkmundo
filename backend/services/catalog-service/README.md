# Katalog-Service

Java 25, Quarkus und eine eigene PostgreSQL-Datenbank. Flyway legt das Schema beim Start an.

```sh
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/catalog
export QUARKUS_DATASOURCE_USERNAME=catalog
export QUARKUS_DATASOURCE_PASSWORD=…
export KEYCLOAK_AUTH_SERVER_URL=http://localhost:8090/realms/handwerker-realm
./mvnw quarkus:dev
```

Der Dienst startet lokal auf Port 8083. Anfragen benötigen ein Bearer-Token. Der
Katalog wird nach dem `sub` des Nutzers getrennt.

## Katalog importieren

Die Testdatei [Hager_Artikelstamm_2026-11.csv](../../../documents/Hager_Artikelstamm_2026-11.csv)
basiert auf dem [Hager-Artikelstamm vom November 2026](https://hager.com/de/support/downloads/artikelstamm-und-preise).
Die Preise sind Bruttopreise pro Mengeneinheit in EUR.

```sh
curl --fail-with-body -H "Authorization: Bearer $TOKEN" \
  -F 'file=@../../../documents/Hager_Artikelstamm_2026-11.csv;type=text/csv' \
  http://localhost:8083/catalog/material/import/csv
```

## CSV-Format

Eine kleine Datei mit erfundenen Artikeln steht unter
[catalog-example.csv](../../../frontend/public/catalog-example.csv) bereit.
Die konvertierte Hager-Testdatei aus #63 bleibt mit diesem Format kompatibel;
unveränderte Herstellerexporte benötigen gegebenenfalls eine vorherige Konvertierung.

- Kodierung: UTF-8, optional mit BOM. Trennzeichen: Semikolon (`;`).
- Die erste Zeile enthält die Spaltennamen, exakt wie unten angegeben.
  Ihre Reihenfolge ist frei. Doppelte oder leere Spaltennamen sind ungültig.
  Weitere benannte Spalten werden ignoriert.
- Jeder Datensatz muss genauso viele Werte wie die Kopfzeile enthalten.
  Leere Zeilen werden übersprungen. Mindestens ein Artikel ist erforderlich.
- Umgebende Leerzeichen werden entfernt. Texte mit Semikolon, Zeilenumbruch
  oder Anführungszeichen in doppelte Anführungszeichen setzen; enthaltene
  Anführungszeichen verdoppeln (`""`). Nullzeichen sind ungültig.

| Spalte | Pflichtspalte | Wert |
| --- | --- | --- |
| `articleNumber` | Ja | Nicht leer, höchstens 255 Zeichen; innerhalb der Datei eindeutig. Groß-/Kleinschreibung wird unterschieden, führende Nullen bleiben erhalten. |
| `name` | Ja | Nicht leere Artikelbezeichnung. |
| `unit` | Ja | Nicht leere Einheit, z. B. `Stück` oder `Meter`. |
| `price` | Ja | Nicht negativer Einzelpreis pro Einheit. Höchstens 18 Vor- und 12 Nachkommastellen. Punkt oder Komma als Dezimalzeichen, keine Tausendertrennzeichen, Währungszeichen oder Exponentialschreibweise. `0`, `4,95` und `1.723` sind gültig; `1.723` bedeutet 1,723, nicht 1723. |
| `manufacturer` | Nein | Darf fehlen oder leer sein; Standard leerer Text. |
| `description` | Nein | Darf fehlen oder leer sein; Standard leerer Text. |
| `category` | Nein | Darf fehlen oder leer sein; Standard leerer Text. |
| `currency` | Nein | Darf fehlen oder leer sein; Standard `EUR`. Sonst genau drei Großbuchstaben, z. B. `EUR`. |

Preise werden ohne Rundung übernommen. Die CSV enthält keine zusätzliche
Steuerinformation; die Hager-Testdatei verwendet Bruttopreise.

## Antworten und Validierung

Erfolg: HTTP 200 mit der Anzahl übernommener Artikel als JSON-Zahl.
Bei einem Dateifehler antwortet der Dienst mit HTTP 400 und einem JSON-Objekt,
z. B. `{"message":"Zeile 3: Doppelte Artikelnummer 'BEISPIEL-001'"}`.
Die Prüfung stoppt beim ersten Fehler. Artikelfehler nennen die physische
Dateizeile (Kopfzeile = 1); bei mehrzeiligen Datensätzen deren letzte Zeile.
Datei- und Kopfzeilenfehler werden ohne Artikelzeile gemeldet.

Erst nach erfolgreicher Prüfung der gesamten Datei werden Artikel gespeichert.
Ungültige Dateien ändern keine vorhandenen Artikeldaten. Beim erneuten Import
ersetzt die Datei den gesamten Katalog des Handwerkers. Artikel, deren Nummer
weiter enthalten ist, behalten ihre UUID; nicht mehr enthaltene Artikel werden
entfernt. Gespeicherte Angebotspositionen enthalten eigene Angaben und Preise
und werden dabei nicht verändert.

Die Format- und Validierungstests laufen ohne Datenbank mit `./mvnw test`.

Für den HTTP-Integrationstest die Datenbankvariablen auf eine separate
Testdatenbank setzen und `CATALOG_TEST_DATABASE=true ./mvnw verify` ausführen.

## Aktiver Katalog und Artikelsuche (#145–#148)

Der Import wird innerhalb einer Datenbanktransaktion vollständig übernommen.
Eine transaktionsgebundene PostgreSQL-Advisory-Sperre pro Handwerker serialisiert
auch zeitgleiche Erstimporte. Andere Handwerker können unabhängig importieren.
Leser sehen bis zum Commit den letzten vollständigen Katalog. Datenbankfehler
rollen auch vorherige Änderungen und Löschungen zurück. Bei Verbindungsabbruch
nach dem Commit kann die Antwort verloren gehen; erneuter Upload ist möglich.

- `GET /catalog/material/search?q=Steckdose&limit=15`: `{ "candidates": [...] }`
  mit ID, Artikelnummer, Bezeichnung, Hersteller, Beschreibung, Kategorie und
  Einheit, ohne Preise. Teilstringsuche ohne Beachtung der Groß-/Kleinschreibung;
  `limit` muss zwischen 1 und 100 liegen.
- `GET /catalog/material/{id}`: eigener Artikel inklusive Einzelpreis und Währung,
  sonst HTTP 404. Der vorhandene Offer-Service-Client nutzt diesen Abruf.

Normale Benutzer werden immer über ihren Token-Subject zugeordnet; ein
`X-Handwerker-Id`-Header kann diese Zuordnung nicht überschreiben. Ausschließlich
technische Tokens mit der Rolle `process-engine` dürfen für Suche und Einzelabruf
einen Handwerker über diesen verpflichtenden Header auswählen. Der bestehende
AI-REST-Client unterstützt diesen Vertrag; sein Mock-Modus muss zur echten
KI-Katalogsuche ausgeschaltet und sein technischer Keycloak-Client eingerichtet
sein (`CATALOG_MOCK_ENABLED=false`, `AI_TECHNICAL_TOKEN_ENABLED=true`). Im aktuellen
Deployment sind Mock und Direktmodus aktiv. Der Offer-Direktmodus übernimmt
KI-Positionen ohne Katalog-ID und ohne Preis. Die Umstellung dieses Ablaufs ist
hier nicht enthalten; nur die bestehenden Such- und Detailverträge werden bedient.
Vor einer Preisübernahme muss außerdem die Preisbasis abgestimmt werden:
Die Hager-Testdatei enthält Bruttopreise, Angebote verwenden Nettoeinzelpreise.
