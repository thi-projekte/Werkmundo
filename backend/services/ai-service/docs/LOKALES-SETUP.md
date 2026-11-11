# ai-service im lokalen Gesamt-Setup

## Variante A – allein starten (empfohlen zum Entwickeln)

```bash
cd Ai_Service
docker compose up --build -d
./beispiele/test-lokal.sh
```

Der Container erreicht Process Engine (`8085`), catalog-service (`8083`) und Keycloak (`8090`) des
Gesamt-Setups über `host.docker.internal`, falls diese laufen. Ohne Process Engine funktioniert
`POST /ai/analyze` vollständig; `POST /ai/process` antwortet mit 202 und loggt danach
„Process Engine nicht erreichbar“.

## Variante B – in `local-setup/docker-compose.local.yml` aufnehmen (Issue #40)

Vorschlag für den Block unter `services:` (Pfad relativ zu `local-setup/`, ggf. anpassen, sobald
der Service im Repo unter `backend/services/ai-service` liegt):

```yaml
  # ---------------------------------------------------------------------------
  # AI-Service (zustandslos; ohne MEGALLM_API_KEY -> Stub-Ergebnis)
  # ---------------------------------------------------------------------------
  ai-service:
    build:
      context: ../backend/services/ai-service
    container_name: cv-ai-service
    restart: unless-stopped
    environment:
      HTTP_PORT: "8086"
      MEGALLM_API_KEY: ${MEGALLM_API_KEY:-}
      CATALOG_MOCK_ENABLED: ${CATALOG_MOCK_ENABLED:-false}
      CATALOG_SERVICE_URL: http://catalog-service:8080
      CAMUNDA_ENGINE_URL: ${CAMUNDA_ENGINE_URL:-http://host.docker.internal:8085/engine-rest}
    extra_hosts:
      - "host.docker.internal:host-gateway"
    ports:
      - "8086:8086"
    depends_on:
      - catalog-service
```

Im Gesamt-Setup ist der catalog-service lokal ohne Auth erreichbar
(`QUARKUS_OIDC_TENANT_ENABLED=false`), daher kann dort `CATALOG_MOCK_ENABLED=false` genutzt werden –
dann bekommt der offer-service echte Katalog-IDs und damit Preise.

In `local-setup/.env` ergänzen (optional):

```env
MEGALLM_API_KEY=
```

Zusätzlich im Kopfkommentar von `docker-compose.local.yml` den Satz „Nicht enthalten: … ai-service“ anpassen.
