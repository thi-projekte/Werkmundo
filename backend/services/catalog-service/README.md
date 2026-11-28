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
aktualisiert dieselbe Artikelnummer die bestehenden Daten des Handwerkers und
behält ihre UUID.

Die Format- und Validierungstests laufen ohne Datenbank mit `./mvnw test`.

Für den HTTP-Integrationstest die Datenbankvariablen auf eine separate
Testdatenbank setzen und `CATALOG_TEST_DATABASE=true ./mvnw verify` ausführen.
