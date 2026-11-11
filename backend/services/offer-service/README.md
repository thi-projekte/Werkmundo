# Offer-Service

Java 25 / Quarkus 3.40.1.1. Ein Maven-Modul enthält die bisherigen
Dashboard-, Rechnungs-, Sprach- und Prozessfunktionen sowie die strukturierte
Auftraggeber-API. Im PostgreSQL-Betrieb verwenden beide Angebots-APIs dasselbe
Repository-Datenmodell und denselben Angebotsbestand.

## Schnittstellen

Die bisherigen Verträge unter `/offers`, `/angebote`, `/dashboard`,
`/rechnungen` und `/speech-capture` bleiben als Kompatibilitätsschnittstellen
erhalten. Ihre Datenformate und Prozessstatus werden nicht pauschal umbenannt.

Die strukturierte Auftraggeber-API liegt unter `/api/offers`:

| Methode | Pfad relativ zu `/api/offers` | Zweck |
|---|---|---|
| POST / GET | leer | Angebot mit Business-Key anlegen / eigene Angebote auflisten |
| GET / PUT / DELETE | `/{businessKey}` | Eigenes Angebot lesen, ändern oder löschen |
| POST | `/{businessKey}/approve` | Freigeben und Kundenzugriffstoken ausstellen |
| POST | `/{businessKey}/sent` | Versandstatus setzen |
| POST | `/{businessKey}/cancel` | Stornieren |
| GET | `/{businessKey}/customer` | Kundenzugriff mit Token |
| POST | `/{businessKey}/customer/accept` | Annehmen |
| POST | `/{businessKey}/customer/reject` | Ablehnen |
| POST | `/{businessKey}/customer/changes` | Änderungswunsch übermitteln |

Handwerkeraktionen prüfen die authentifizierte Identität und den Besitzer.
Die strukturierte Kunden-API erwartet `X-Offer-Access-Token`; sie speichert
Hash und Ablaufzeit. Der bestehende Annahmelink verwendet weiterhin den
bisherigen separaten Annahmetoken. Eine Vereinheitlichung ist noch offen.

`OfferStatusMapper` übersetzt die Prozesszustände. Bei unverändertem
fachlichem Status bleibt der genauere Prozessstatus erhalten. Laufende
KI-Verarbeitung wird als `PROCESSING` abgebildet und ist kein editierbarer
Entwurf. Anfahrtspositionen werden als `TRAVEL` abgebildet.

## Persistenz und Migration

`OfferJpaRepository` verwendet die bestehenden Entity-Klassen unter
`de.winfprojekt.craftvoice.offerservice`. Die vorherigen separaten
Squad-Entity-Klassen sind entfernt. Maßgebliche Tabellen sind `offer`,
`offer_position`, `offer_status_history` und ihre ergänzenden Tabellen.
Rechnungen liegen weiterhin in derselben fachlichen Offer-Datenbank.
Dokumente besitzen eine eigene Datenbank im Document-Service.

V1 bleibt unverändert. V2 ersetzt ausschließlich leere Squad-Ausgangstabellen
durch das gemeinsame Schema. Enthält eine dieser Tabellen Datensätze, bricht
V2 ab. V2 ist keine Datenmigration für gefüllte Squad-Datenbanken und keine
Übernahmeanleitung für eine bestehende Serverdatenbank aus `main`.
Vor einer solchen Übernahme ist eine gesonderte Datenmigration erforderlich.

Hibernate validiert das Flyway-Schema. Die explizite Snake-Case-Namensstrategie
muss mit den in der Migration definierten Spaltennamen übereinstimmen.
Angewendete Migrationen dürfen nicht nachträglich geändert werden.

## Prüfung

Im Repository-Hauptordner mit Java 25 und laufendem Docker Desktop:

```powershell
.\scripts\Test-Auftraggeber.ps1
```

Das Skript startet die getrennten PostgreSQL-Testdatenbanken. Erforderliche
Passwörter werden aus der Umgebung übernommen oder abgefragt. HTTP-Regressionen
verwenden ein isoliertes H2-Profil; die Persistenztests verwenden ausdrücklich
`OfferPostgresTestProfile` und PostgreSQL. Fehlende oder übersprungene
Datenbanktests führen im Gesamtskript zu einem Fehler.

Lokal am 11.11.2026 nachgewiesen: 169 Offer-Tests und 11 Document-Tests,
jeweils ohne Fehler oder übersprungene Tests; beide Builds erfolgreich.
Die Tests prüfen unter anderem den gemeinsamen Angebotsbestand und den Erhalt
von Prozessstatus und Historiennotizen. Das ist kein vollständiger Nachweis
eines Ablaufs mit Browser, echten Keycloak-Tokens und Process Engine.

## Verbleibende Arbeit

- Gemeinsames lokales Startsetup mit Java 25 und passenden Datenbankvariablen.
- Echte OIDC-, Browser- und Prozessintegration sowie technische Caller-Rechte.
- Absicherung paralleler Angebotsänderungen gegen verlorene Aktualisierungen.
- Angebotsnummern und fachliche Dokumentversionen.
- Auftragsbestätigung und vollständiger bearbeitbarer Rechnungsablauf.
- Produktive PDF-Erzeugung und Versand im Document-Service.

Der In-Memory-Adapter ist nur eine Entwicklungs-/Testalternative und kein
Nachweis für gemeinsame persistente Speicherung mit den bisherigen Funktionen.
