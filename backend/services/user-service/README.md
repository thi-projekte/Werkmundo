# User Service

Der `user-service` verwaltet lokale Profil- und Unternehmensdaten sowie Kundenprofile. Keycloak ist als OIDC-Identity-Provider und Admin-Client integriert. Es handelt sich um Java 17 / Quarkus 3.15.1 (älter als die neueren Module) mit PostgreSQL im Compose-Stack.

## API-Übersicht

Basispfad: `/api/users`. Requestdetails, Validierung und Responses stehen in `UserResource.java` und `UserService.java`.

| Methode | Pfad | Hinweis |
|---|---|---|
| `POST` | `/api/users/register` | Registrierung; `@PermitAll` |
| `GET` | `/api/users/me` | eigenes Profil synchronisieren/lesen; authentifiziert |
| `PUT` | `/api/users/profile` | Profil aktualisieren; authentifiziert |
| `PUT` | `/api/users/company` | Unternehmensdaten; Rolle `OWNER` |
| `GET` | `/api/users/profile/hourly-rate` | Stundensatz; `OWNER` oder `EMPLOYEE` |
| `GET/PUT` | `/api/users/profile/travel-config-detailed` | Reisekostenkonfiguration; `OWNER` oder `EMPLOYEE` |
| `GET` | `/api/users/profile/travel-config` | kompakte Reisekostenkonfiguration; `OWNER` oder `EMPLOYEE` |
| `POST` | `/api/users/profile-picture` | Profilbild als Multipart-Feld `file`; authentifiziert |
| `POST` | `/api/users/password-reset/initiate` | Passwortreset anstoßen; `@PermitAll` |
| `DELETE` | `/api/users` | eigenes Konto löschen; `OWNER` |
| `POST/GET` | `/api/users/customers` | Kunden anlegen/auflisten; `OWNER` oder `EMPLOYEE` |
| `GET/PUT/DELETE` | `/api/users/customers/{id}` | Kunden abrufen/ändern/löschen; `OWNER` oder `EMPLOYEE` |

Die Annotationen oben entsprechen dem aktuellen Resource-Code. Realm-/Client-Rollen und globale HTTP-Policy müssen zusätzlich in der Keycloak-/Quarkus-Konfiguration übereinstimmen. Nicht alle Rollen- und Synchronisationsdetails aus der früheren README sind durch den Endpoint-Code allein garantiert.

## Lokale Entwicklung und Konfiguration

`src/main/resources/application.properties` liest Laufzeit-Zugangsdaten aus der Prozessumgebung. Für einen lokalen Quellcode-Start mit PostgreSQL und Keycloak müssen die folgenden Variablen in der PowerShell-Sitzung gesetzt sein:

| Variable | Verwendung |
|---|---|
| `DB_HOST` | PostgreSQL-Hostname, bei lokal veröffentlichtem Port typischerweise `localhost` |
| `DB_PORT` | PostgreSQL-Port; optional, Standard ist `5432` |
| `DB_NAME` | Datenbankname |
| `DB_USER` | Datenbankbenutzer |
| `DB_PASSWORD` | Datenbankpasswort |
| `KEYCLOAK_URL` | Basis-URL der lokalen Keycloak-Instanz ohne `/realms/...` |
| `USER_SERVICE_OIDC_SECRET` | Client-Secret für den OIDC-Client `user-service` |
| `KEYCLOAK_ADMIN_USER` | Benutzer des Keycloak-Admin-Clients |
| `KEYCLOAK_ADMIN_PASSWORD` | Passwort dieses Admin-Benutzers |
| `KEYCLOAK_USER_REALM` | Ziel-Realm; optional, Standard ist `handwerker-realm` |

Beispiel für eine einzelne PowerShell-Sitzung. Ersetze jeden Platzhalter lokal durch den Wert deiner vorhandenen Entwicklungsumgebung; trage keine echten Werte in diese README oder andere versionierte Dateien ein:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "<DB_NAME>"
$env:DB_USER = "<DB_USER>"
$env:DB_PASSWORD = "<DB_PASSWORD>"
$env:KEYCLOAK_URL = "http://localhost:8090"
$env:KEYCLOAK_USER_REALM = "handwerker-realm"
$env:USER_SERVICE_OIDC_SECRET = "<USER_SERVICE_OIDC_SECRET>"
$env:KEYCLOAK_ADMIN_USER = "<KEYCLOAK_ADMIN_USER>"
$env:KEYCLOAK_ADMIN_PASSWORD = "<KEYCLOAK_ADMIN_PASSWORD>"

Set-Location backend\services\user-service
.\mvnw.cmd quarkus:dev "-Dquarkus.http.port=8091"
```

`KEYCLOAK_URL` muss auf den tatsächlich lokal veröffentlichten Keycloak-Port zeigen. Verwende für `DB_HOST` bei einem Service innerhalb eines Docker-Netzwerks dessen Compose-Servicenamen statt `localhost`. Die lokalen Werte werden hier nicht festgelegt. Umgebungsvariablen gelten für die aktuelle PowerShell-Sitzung und deren gestartete Prozesse; nach Ende der Sitzung werden sie nicht dauerhaft gespeichert.

Die Testkonfiguration verwendet weiterhin H2 und die vorhandenen Test-Profil-Einstellungen. Sie benötigt keine produktiven Zugangsdaten. Das ersetzt keine Integrationstests gegen echte PostgreSQL- und Keycloak-Instanzen.

### Verbleibende Keycloak-Sicherheitsarbeit

Der Admin-Client ist weiterhin auf den Realm `master` und den Passwort-Grant konfiguriert. Damit hängt der mögliche Berechtigungsumfang von dem gesetzten Admin-Konto ab; ein Master-Admin wäre für den Dienst zu privilegiert. Die Umstellung auf einen dedizierten Service-Account-Client mit minimalen Rollen (beispielsweise nur die tatsächlich benötigten Benutzerverwaltungsrechte) muss mit der Keycloak-Konfiguration abgestimmt werden und ist in dieser Änderung nicht vorgenommen worden.

Die zuvor versionierten Datenbank- und Keycloak-Zugangsdaten könnten bereits bekannt oder offengelegt worden sein. Entfernen aus der Konfiguration widerruft sie nicht: prüfe die Git-Historie und rotiere betroffene Zugangsdaten beziehungsweise Client-Secrets über die zuständigen Konten. Es wurden weder Konten noch Passwörter geändert.

Der Testkontext verwendet H2 und deaktiviert OIDC, um externe Keycloak-Verfügbarkeit zu vermeiden. Das ist kein Nachweis, dass produktive Rollen oder Keycloak-Flows integriert funktionieren.

## Zuständigkeit und Grenzen

Der Service synchronisiert den angemeldeten Benutzer, verwaltet Kunden-/Unternehmensfelder und stellt Profilwerte bereit, die der Offer Service zur Berechnung nutzen kann. Angebots- und Prozesszustände gehören nicht in diese Datenbank. Weitere Gesamtarchitektur: [Backend-Service-Index](../README.md) und [Übergabehandbuch](../../../docs/PROJECT-HANDBOOK.md).
