# Lokales Setup

1. `local-setup/.env.example` nach `local-setup/.env` kopieren und bei Bedarf
   die lokalen Passwörter anpassen.
2. Im Repository-Root ausführen:

   ```powershell
   docker compose --env-file local-setup/.env -f local-setup/docker-compose.local.yml up --build
   ```

Wenn bereits ein früherer Start mit anderen lokalen Passwörtern erfolgt ist,
einmalig die Datenvolumes zurücksetzen:

```powershell
docker compose --env-file local-setup/.env -f local-setup/docker-compose.local.yml down -v
```

3. Das Frontend ist unter <http://localhost:5173> erreichbar, Keycloak unter
   <http://localhost:8090>.

Das Setup baut `user-service` und `offer-service` aus
`backend/services` lokal. `catalog-service` und `document-service` sind
standardmäßig nicht aktiviert, weil ihre Verzeichnisse derzeit noch keine
ausführbaren Maven-/Docker-Projekte enthalten. Die KI- und Process-Engine-
Funktionen benötigen zusätzlich externe Zugangsdaten beziehungsweise Server.

Die Frontend- und Basisdienste starten damit lokal. Für einen vollständigen
Katalog-/Dokumentbetrieb müssen diese beiden Services zuerst als ausführbare
Projekte ergänzt oder über passende Images bereitgestellt werden.
