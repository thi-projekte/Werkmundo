# ai-service (Werkmundo · Squad Handwerker)

Der ai-service wandelt die Spracheingabe eines Handwerkers in strukturierte Angebotspositionen um.
Er ist der neu strukturierte AI-Service aus dem alten Projekt `CraftVoice/backend/services/ai-service`,
angepasst an die Systemarchitektur und die Zielstruktur aus Issue #29 (Issue #39 „AI-Service anpassen“).

- **Zustandslos**, keine eigene Datenbank
- Aufruf durch die **Process Engine** (HTTP-Connector) → Antwort per Message `ergebnisKI`
- Läuft **lokal ohne Keys**: ohne `MEGALLM_API_KEY` kommt ein Stub-Ergebnis, statt des catalog-service wird ein Mock-Katalog genutzt

---

## Schnellstart (lokal)

Voraussetzung: nur **Docker** (Java/Maven werden nicht lokal gebraucht).

```bash
cd Ai_Service
docker compose up --build -d
./beispiele/test-lokal.sh
```

| Was | URL |
|---|---|
| Health | http://localhost:8086/q/health |
| Analyse synchron (zum Testen) | `POST http://localhost:8086/ai/analyze` |
| Analyse für die Process Engine | `POST http://localhost:8086/ai/process` |
| Sprachaufnahme → Text (vom Frontend) | `POST http://localhost:8086/ai/transcribe` (multipart, Feld `audio`) |

Mit echter KI: `cp .env.example .env`, `MEGALLM_API_KEY=...` eintragen, `docker compose up -d` neu starten.

Stoppen: `docker compose down`

### Alternative: mit Java 21 (Hot-Reload)

```bash
./mvnw quarkus:dev      # startet auf Port 8086
./mvnw test             # 72 Tests
```

Ohne lokales Java laufen die Tests auch per Docker:

```bash
docker run --rm -v "$PWD":/app -v ai-service-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn -B test
```

---

## Ablauf

```text
Frontend ──POST /ai/transcribe (Audio)──▶ ai-service ──▶ Deepgram   → {"transkript": "..."}
   │ Transkript prüfen/bearbeiten
   ▼
Frontend → offer-service → Process Engine ──POST /ai/process──▶ ai-service
                                ▲                                   │ 202 sofort
                                │                                   ▼ (Hintergrund)
                                │                        LLM-Call 1: Sprachschnipsel → Positionen
                                │                        LLM-Call 2: je Material → Katalogprodukt
                                └──── POST /engine-rest/message ────┘
                                      messageName = ergebnisKI, businessKey
```

- **LLM-Call 1 (Extraktion):** Sprachschnipsel + Vorlage (bzw. bestehende Positionen + Korrektur) → `leistungen`, `material`, `notizen`, `korrekturvorschlaege`, `geschaetzteArbeitsdauerStunden`
- **LLM-Call 2 (Produktauswahl):** pro Materialposition Kandidaten aus dem Katalog suchen, das LLM wählt genau einen oder `KEIN_TREFFER` → `katalogProduktId`
- **Datenschutz:** Preise gehen nie an die KI. Der offer-service ergänzt sie über die `katalogProduktId`.
- **Fallbacks:** ohne LLM / bei LLM-Fehler → Stub (Call 1) bzw. Top-Suchtreffer (Call 2); Modell-Fallback `gemini-3-flash-preview` → `google-gemma-4-26b`.

Schnittstellen als JSON-Schema: [contracts/ai/AiAnalysisRequest.json](contracts/ai/AiAnalysisRequest.json),
[contracts/ai/AiAnalysisResult.json](contracts/ai/AiAnalysisResult.json),
[contracts/workflow/ErgebnisKiMessage.json](contracts/workflow/ErgebnisKiMessage.json).

---

## Struktur

Gleiche Schichten wie bei den anderen Services der neuen Architektur (`api / application / domain / infrastructure`):

```text
Ai_Service/
├── pom.xml, mvnw, Dockerfile, docker-compose.yml, .env.example
├── docker/Dockerfile.jvm   Image-Build der Backend-CI (nach mvn package)
├── contracts/                 Schnittstellen (JSON-Schema) → gehören nach Beispiel/contracts/
├── beispiele/                 Beispiel-Payloads + test-lokal.sh
├── docs/                      Umsetzung Issue #29, offene Punkte, Referenzdokumente
└── src/main/java/de/winfprojekt/werkmundo/aiservice/
    ├── api/                   REST: AiAnalysisResource, Exception-Mapper
    │   └── dto/               AiAnalysisRequest, AcceptedResponse, ErrorResponse
    ├── application/           AnalyzeVoiceInputUseCase (Call 1 → Call 2 → Engine)
    │   │                      LlmCall1Generator, Call1PromptBuilder, ErgebnisKiParser,
    │   │                      Call2Selector, Call2PromptBuilder, Call2AnswerParser,
    │   │                      StubResultGenerator, PromptTemplates, LlmException
    │   └── port/              LlmPort, CatalogSearchPort, WorkflowNotifier (Interfaces)
    ├── domain/                AnalyseAuftrag, ProcessType, Vorlage, Angebotspositionen,
    │                          Position, ErgebnisKi, ProduktKandidat
    └── infrastructure/
        ├── llm/               MegaLlmAdapter (Retry, Modell-Fallback), MegaLlmClient
        ├── catalog/           MockCatalogSearch, RestCatalogSearch, Auswahl per Config
        ├── processengine/     CamundaWorkflowNotifier, CamundaMessageClient
        └── auth/              TechnicalTokenFilter (Keycloak client_credentials, optional)
src/main/resources/
    ├── application.properties
    ├── prompts/               System-Prompts als Textdateien (ohne Java änderbar)
    └── mock/catalog-mock.json
```

Abhängigkeiten zeigen nur nach innen: `api → application → domain`, `infrastructure → application/domain`.
Die application-Schicht kennt weder MegaLLM noch Camunda noch REST-Clients – nur die Ports.

---

## Konfiguration

Alles per Env-Variable (Vorlage: [.env.example](.env.example)). Wichtigste Werte:

| Variable | Default (lokal) | Bedeutung |
|---|---|---|
| `HTTP_PORT` | `8086` | Port (8081–8085 und 8090 belegt das lokale Setup) |
| `MEGALLM_API_KEY` | leer → Stub | API-Key für MegaLLM, **nie committen** |
| `DEEPGRAM_API_KEY` | leer → Beispiel-Transkript | Speech-to-Text für `/ai/transcribe`, **nie committen** |
| `AI_CORS_ORIGINS` | `http://localhost:5173` | Frontend-Origins für `/ai/transcribe` |
| `AI_TRANSCRIBE_AUTH_POLICY` | `permit` | online `authenticated`, sobald das Frontend Tokens schickt |
| `MEGALLM_MODEL` / `MEGALLM_MODEL_FALLBACK` | `gemini-3-flash-preview` / `google-gemma-4-26b` | Modellwahl |
| `CATALOG_MOCK_ENABLED` | `true` | `false` = echter catalog-service |
| `CATALOG_SERVICE_URL` | `http://localhost:8083` | catalog-service |
| `CAMUNDA_ENGINE_URL` | `http://localhost:8085/engine-rest` | Process Engine |
| `AI_SECURITY_ENABLED` + `AI_AUTH_POLICY=process-engine` | aus / `permit` | eingehende Aufrufe nur mit Keycloak-Token (Rolle `process-engine`) |
| `AI_TECHNICAL_TOKEN_ENABLED` + `AI_SERVICE_OIDC_SECRET` | aus | technisches Token für Process Engine / catalog-service |

Die Online-Konfiguration (Sicherheit, technisches Token) ist vorbereitet, aber laut Issue #39 erst Ziel eines späteren Sprints.

---

## Weitere Dokumente

- [docs/DEPLOYMENT-PORTAINER.md](docs/DEPLOYMENT-PORTAINER.md) – Server-Deployment: Portainer-Stack, Secrets, Nginx Proxy Manager
- [docs/UMSETZUNG-ISSUE-29.md](docs/UMSETZUNG-ISSUE-29.md) – jeder TODO-Kommentar aus der Zielstruktur und wie er erledigt wurde
- [docs/OFFENE-PUNKTE.md](docs/OFFENE-PUNKTE.md) – Abstimmungen mit den anderen Squads
- [docs/LOKALES-SETUP.md](docs/LOKALES-SETUP.md) – Einbindung in `local-setup/docker-compose.local.yml` (Issue #40)
- [docs/referenz/](docs/referenz/) – Modellwahl, Datenvertrag und KI-Übersicht aus dem Vorsemester (Stand alter Code)
