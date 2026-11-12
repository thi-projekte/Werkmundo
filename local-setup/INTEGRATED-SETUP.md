# Gemeinsamer lokaler Start

Docker Desktop starten, danach im Repository-Root:

```powershell
.\Start-Local.cmd
```

Erster Start: Images werden gebaut, lokale Passwörter automatisch erzeugt.
Die Datei `.env.local` ist privat und wird ignoriert. Alte generierte Dateien unter `.runtime/` werden nicht mehr verwendet und bleiben ebenfalls ignoriert.
Die Kennwörter für weitere Starts beibehalten. Keine Serverzugangsdaten verwenden.

Nur Konfiguration prüfen: `.\Start-Local.cmd -ValidateOnly`.
Ohne optionalen AI-Service: `.\Start-Local.cmd -WithoutAi`.

Die zusätzliche Compose-Datei erweitert Patricks ursprüngliche Datei.
Seine Compose-Datei, sein Realm-Import und sein Offer-Dockerfile bleiben unverändert.
Für Offer wird ein eigener Java-25-Dockerfile verwendet.

Frontend: http://localhost:5173; Keycloak: http://localhost:8090.
User: Port 8081; Offer: Port 8082; Document: Port 8084; AI: Port 8086.
Die Datenbanken für User, Offer und Document sind getrennte Container und Volumes.
Sie haben keine veröffentlichten Host-Ports. Die Testdatenbanken auf 55432 und
55433 bleiben davon unabhängig und ihre Daten werden nicht übernommen.

Keycloak verwendet Patricks unveraenderten Realm-Import und ein eigenes lokales Volume. Das Startskript erstellt keine zusaetzlichen Benutzer, Clients oder Rollen und aendert den Realm nicht.
Lokale Konten und Dienste haben keine bestätigte vollständige Browserintegration.
Containerstart allein ist kein Nachweis eines funktionierenden Geschäftsablaufs.

AI startet standardmäßig ohne Provider-Key mit seinem dokumentierten Ersatzbetrieb
und Mock-Katalog. Process Engine und Katalogimplementierung werden nicht mitgeliefert.
PDF-Erzeugung und Versand fehlen weiterhin. Die bestehende Angebotserstellung,
die eine Process Engine erwartet, ist damit noch kein vollständiger lokaler Ablauf.

Zum Stoppen ohne Datenverlust im Repository-Root:

```powershell
docker compose --env-file local-setup/.env.local -f local-setup/docker-compose.local.yml -f local-setup/docker-compose.integrated.yml --profile ai stop
```

Die Startdateien löschen keine Volumes. Kein `down -v` verwenden, wenn Daten
behalten werden sollen. Änderungen am Realm-Import aktualisieren einen bereits
importierten Realm nicht automatisch.

## Bereinigung der lokalen Authentifizierungskonfiguration

Die zusaetzlichen Testkonten und Client-Aenderungen sind aus dem Startskript entfernt. Das neue Volume local_keycloak_original_data wird mit dem unveraenderten urspruenglichen Realm initialisiert. Das zuvor verwendete lokale Testvolume wird nicht geloescht, aber nicht mehr eingebunden. Vollstaendige Keycloak-/OIDC-Integration und Token-Tests bleiben Aufgabe des zustaendigen Teams.
