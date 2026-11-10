# Document-Service

Java 25 / Quarkus 3.40.1.1. Der aktuelle Stand enthält REST-Endpunkte,
Besitzerprüfungen, ein PostgreSQL-Repository und eine Flyway-Migration für
Dokumentmetadaten und PDF-Inhalte. Er ist noch kein vollständig integrierter
PDF- und Versandservice.

## Datenbank

Die Migration `src/main/resources/db/migration/V1__create_documents.sql`
legt die Tabelle `documents` an. Sie enthält Besitzer, Dokumenttyp,
Referenz, Empfänger, Dateiname, PDF-Bytes (`bytea`) und Erstellungszeitpunkt.
Abfragen sind nach Besitzer getrennt. SQL-Parameter werden gebunden.

Damit sind Schema und Speicherimplementierung vorhanden. Eine eigene
laufende PostgreSQL-Datenbank und deren Bereitstellung sind in diesem Branch
noch nicht enthalten. Die Datenbank des Offer-Service ist dafür nicht vorgesehen.

Für eine separat bereitgestellte PostgreSQL-Datenbank konfigurieren:

| Umgebungsvariable | Zweck |
|---|---|
| `DOCUMENT_STORAGE_POSTGRES_ENABLED=true` | Repository, Datenquelle und Migration aktivieren |
| `QUARKUS_DATASOURCE_JDBC_URL` | JDBC-URL der eigenen Dokumentdatenbank |
| `QUARKUS_DATASOURCE_USERNAME` | Datenbankbenutzer |
| `QUARKUS_DATASOURCE_PASSWORD` | Kennwort zur Laufzeit |

Ohne aktivierte PostgreSQL-Speicherung gibt es außerhalb der Tests keine
alternative Speicherimplementierung. Speicherendpunkte antworten dann mit
`503 Service Unavailable`.

## Schnittstellen

Alle Endpunkte erfordern eine authentifizierte Identität.

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/documents` | Metadaten eigener Dokumente |
| GET | `/documents/{documentId}` | Metadaten eines eigenen Dokuments |
| GET | `/documents/offers/{businessKey}` | Metadaten des neuesten eigenen Angebotsdokuments |
| GET | `/documents/{documentId}/pdf` | PDF eines Rechnungsdokuments; Angebote werden derzeit abgewiesen |
| POST | `/documents/invoices/{businessKey}/generate` | Rechnungsdokument aus `rechnungsentwurf` erzeugen |
| POST | `/documents/offers/{businessKey}/share` | Angebotsdokument versenden |
| POST | `/documents/invoices/{businessKey}/share` | Rechnungsdokument versenden |

Der Rechnungsentwurf wird als JSON-Objekt mit
`rechnungsentwurf.kundendaten.email` erwartet. Erzeugung und Versand brauchen
die unten genannten konkreten Adapter.

## Noch fehlende Bausteine

- `InvoicePdfGenerator`: Es existiert nur das Interface, keine produktive
  PDF-Erzeugung. Der Erzeugungsendpunkt liefert ohne Implementierung `503`.
- `DocumentSender`: Es existiert nur das Interface, keine produktive
  Versandimplementierung. Versand liefert ohne Implementierung `503`.
- Erzeugung von Angebots-PDFs und deren Download sind noch nicht umgesetzt.
- Keycloak/OIDC-Abhängigkeit und Konfiguration fehlen; `@Authenticated`
  allein richtet noch keine Bearer-Token-Prüfung ein.
- Maven Wrapper, Docker-Build und gemeinsamer Startstack fehlen.
- Ein Integrationstest gegen echtes PostgreSQL fehlt.

## Tests

Mit installiertem Maven und Java 25 im Serviceverzeichnis:

```powershell
mvn -B '-Dquarkus.http.test-port=18082' test
```

Die vorhandenen sieben API-Tests verwenden Testidentitäten, ein
In-Memory-Repository, drei feste Bytes als PDF und einen simulierten Sender.
Sie prüfen API-Verhalten und Besitzertrennung. Sie bestätigen weder echte
PDF-Erzeugung oder E-Mail-Zustellung noch das PostgreSQL-Repository.
