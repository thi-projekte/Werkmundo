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

Der Import erwartet UTF-8 mit Semikolon als Trennzeichen. Pflichtspalten sind
`articleNumber`, `name`, `unit` und `price`. Optional sind `manufacturer`,
`description`, `category` und `currency` (Standard EUR). Die Antwort enthält
die Anzahl verarbeiteter Artikel. Beim erneuten Import aktualisiert dieselbe
Artikelnummer die bestehenden Daten des Handwerkers und behält ihre UUID.

Für den HTTP-Integrationstest die Datenbankvariablen auf eine separate
Testdatenbank setzen und `CATALOG_TEST_DATABASE=true ./mvnw verify` ausführen.
