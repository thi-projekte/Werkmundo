# Prüfung der Auftraggeber-Integration

Stand: 10.11.2026. Grundlage sind lokal gespeicherte Remote-Referenzen;
ein aktueller Abruf von GitHub war in der Prüfumgebung nicht möglich.
Pull-Request-Zustand, Reviews und aktuelle CI-Ergebnisse sind damit nicht bestätigt.

| Referenz | Geprüfter Commit |
|---|---|
| `origin/SquadAuftraggeber` | `2a5fa73` |
| `origin/Arne/31_Offer-Service_implementieren` | `db7f483` |
| `origin/marcel/33_Document-Service` | `0a4f215` |
| `origin/main` | `2a52c57` |
| `origin/UI` | `60022fe` |

## Ergebnis

Arnes und Marcels Branches lassen sich auf Basis des geprüften Squad-Stands
ohne Git-Konflikte zusammenführen. Dies wurde in einem separaten lokalen
Prüfbranch durchgeführt. Der ursprüngliche Checkout und die Remote-Branches
wurden nicht geändert.

Die Übernahme von `main` erzeugt dagegen drei Konflikte im Offer-Service:
`pom.xml`, `.mvn/wrapper/maven-wrapper.properties` und
`src/main/resources/application.properties`. Der entsprechende Probe-Merge
wurde abgebrochen. Seine Änderungen sind nicht Teil dieses Prüfbranchs.

Das eigentliche Problem geht über diese Textkonflikte hinaus: `main` enthält
eine andere Implementierung unter `de.winfprojekt.craftvoice.offerservice`.
Arnes neue Implementierung liegt unter `de.werkmundo.offer`. Mehrere REST-Routen
und Datenbanktabellen würden sich überschneiden. Die alte Implementierung
kann deshalb nicht durch bloßes Zusammenführen der POM-Dateien integriert werden.
Benötigte bisherige Funktionen müssen gezielt in die neue Architektur überführt
werden. Bestehende Daten benötigen eine gesondert geprüfte Migration.

## Behobener Fehler

`POST /offers` verwendete bislang dieselbe `save`-Operation wie Änderungen.
Ein neuer Aufruf mit vorhandenem `businessKey` konnte das bestehende Angebot
einschließlich Besitzer und Status überschreiben.

Anlegen verwendet im Prüfbranch eine eigene `create`-Operation. Das
In-Memory-Repository nutzt `putIfAbsent`; PostgreSQL verwendet beim Anlegen
ausschließlich INSERT und die bestehende UNIQUE-Constraint. Bereits vorhandene
Schlüssel liefern einen fachlichen Konflikt. Bei gleichzeitigen Inserts schützt
die Datenbank vor Überschreiben; die einheitliche HTTP-Abbildung eines erst beim
Flush auftretenden UNIQUE-Konflikts ist noch gesondert zu prüfen.

Neue Regressionstests prüfen fremde Besitzer, Zurücksetzen eines bestehenden
Angebots und das In-Memory-Repository. Der ergänzte PostgreSQL-Test konnte in
dieser Umgebung nicht ausgeführt werden.

## Testnachweise und Grenzen

- Unveränderter Offer-Service: 57 Unit-Tests erfolgreich, 0 Fehler,
  0 übersprungene Tests; Server-/Datenbanktests dabei ausdrücklich ausgeschlossen.
- Zwei neue Regressionstests reproduzierten das Überschreiben vor der Korrektur.
- Nach der Korrektur: 60 Unit-Tests erfolgreich, 0 Fehler, 0 übersprungene Tests.
  Server-/Datenbanktests wurden in diesem Lauf ausdrücklich ausgeschlossen;
  ihre Testquellen wurden mitkompiliert, aber nicht ausgeführt.
- Vollständiger Offer-Testlauf in der Prüfumgebung nicht erfolgreich:
  lokale Netzwerkverbindungen sind gesperrt; Quarkus scheitert beim HTTP-Start.
  Dieser Fehler unterscheidet sich vom PostgreSQL-Timeout im ursprünglichen Log.
- Docker-Zugriff ist gesperrt. Der frühere erfolgreiche Einzel-Datenbanktest
  stammt aus dem bereitgestellten Gesprächsverlauf und wurde nicht neu bestätigt.
- Marcels unveränderter Maven-Lauf konnte offline nicht starten:
  Compiler-Plugin 3.14.1 ist nicht vorhanden. Ein separater Versuch mit dem
  vorhandenen Compiler-Plugin 3.15.0 blieb an der fehlenden Test-Security-
  Abhängigkeit hängen. Dies belegt keinen Fehler des Java-Codes.
- Die vorhandene GitHub Action prüft nur Struktur und Dokumentationsdateien,
  keine Service-Builds oder Funktionstests.

## Nächste Integrationsschritte

1. Aktuelle Remote-Branches und Arnes tatsächlichen PR-Stand abrufen.
2. Offer-Service vollständig mit PostgreSQL testen, einschließlich der neuen
   Regression und HTTP-Anfragen mit echten Handwerkeridentitäten.
3. Document-Service bauen und seine API-Tests ausführen; eigene PostgreSQL-
   Instanz und Repository-Integrationstest ergänzen.
4. Keycloak für beide neuen Services einrichten. Die bisherige lokale Basic Auth
   im Offer-Service und Testidentitäten im Document-Service ersetzen das nicht.
5. PDF-Erzeugung, Angebotserstellung im Document-Service und Versand implementieren.
6. UI-Datenverträge, Prozessnachrichten und Serviceadapter auf die neue
   Implementierung abstimmen. Beispiel: UI verwendet `/review/approve`, der neue
   Service `/approve`; UI verwendet `gesamtPreis`, der Service `totalPrice`.
7. Patricks Java-21-Docker-Build, Umgebungsvariablen und Startstack auf die neuen
   Java-25-Services abstimmen. Document-Service ist im bisherigen Standardstack
   nicht aktiv. Neue Flyway-Tabellen dürfen nicht unkontrolliert über bereits
   vorhandene Tabellen der anderen Implementierung gelegt werden.
8. Erst nach erfolgreichen gemeinsamen Tests den Integrations-PR nach `main`
   vorbereiten. Für die Produktion gilt Markus' Architektur: eigene Fachdaten
   je Service, JWT-Prüfung, Prozesssteuerung in CIB seven und öffentlicher Zugriff
   über den Reverse Proxy. Lokale Host-Ports sind kein Produktionsdeployment.

Der Prüfbranch ist eine vorbereitete Arbeitsgrundlage und keine Freigabe für
einen produktiven Betrieb.
