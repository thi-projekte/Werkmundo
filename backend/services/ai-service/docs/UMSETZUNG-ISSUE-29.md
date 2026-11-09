# Umsetzung der Kommentare aus Issue #29 (Zielstruktur) – Issue #39

Quelle: Branch `SquadHandwerker`, Commit `2ca0218` („Issue 29, Zielstruktur festlegen“),
Ordner `Beispiel/backend/services/ai-service/`. Jede Datei dort hatte einen `[Squad P] KOPIERT AUS … TODO`-Kopf.
Alle Köpfe sind in `Ai_Service/` abgearbeitet und entfernt.

## Allgemein (alle Dateien)

| Kommentar | Erledigt |
|---|---|
| Packages umbenennen: `model → domain`, `pipeline → application`, `client → infrastructure` | Ja. Alle Packages, Imports und `{@link}`-Verweise angepasst. |
| Javadoc der `package-info.java` an neue Struktur anpassen | Ja, für Root, `api`, `application`, `application.port`, `domain`, `infrastructure`. |
| Beim Zurückführen in ein Maven-Projekt `resources/` wieder unter `src/main/resources` | Ja. `Ai_Service` ist ein vollständiges Maven/Quarkus-Projekt. |
| pom, Dockerfile, docker-compose, mvnw, Tests wurden nicht mitkopiert | Neu erstellt bzw. übernommen: `pom.xml`, `mvnw`, `Dockerfile` (Multi-Stage, kein lokales Java nötig), `docker-compose.yml`, alle Tests portiert und erweitert. |

## api

| Datei / Kommentar | Erledigt |
|---|---|
| `ProcessResource` 1. Imports anpassen | Ja. Klasse heißt jetzt `api/AiAnalysisResource` (ersetzt den Platzhalter `AiAnalysisResource.java`). |
| `ProcessResource` 2. Orchestrierung raus (`verarbeiteUndKorreliere`, `sendErgebnisKiToCamunda`) | Ja → `application/AnalyzeVoiceInputUseCase` (`startAsync`, `analysiere`). Die Resource übersetzt nur DTO → Domain und ruft den Use-Case. |
| `ProcessResource` 3. `CompletableFuture.runAsync` ohne Quarkus-Context → `ManagedExecutor` | Ja. `AnalyzeVoiceInputUseCase` nutzt `ManagedExecutor` (Extension `quarkus-smallrye-context-propagation`). |
| `ProcessResource` 4. Endpoint ungeschützt → `@RolesAllowed("process-engine")` | Vorbereitet per Konfiguration statt Annotation, damit es lokal ohne Keycloak läuft: HTTP-Permission auf `/ai/*`, Policy `process-engine` (Rolle konfigurierbar). Aktivieren mit `AI_SECURITY_ENABLED=true`, `AI_AUTH_POLICY=process-engine`. Getestet: ohne Token → 401. Online-Betrieb = späterer Sprint. |
| `ProcessResource` 5. `ErrorResponse` in eigene Datei | Ja → `api/dto/ErrorResponse`. Fehlerbehandlung über `api/UngueltigerAuftragMapper` (HTTP 400). |
| `ProcessResource` 6. Vertrag als JSON-Schema in `contracts/ai/` | Ja → `contracts/ai/AiAnalysisRequest.json`, `contracts/ai/AiAnalysisResult.json`, zusätzlich `contracts/workflow/ErgebnisKiMessage.json`. |
| `api/package-info` Verweis `pipeline → application` | Ja. |

## domain

| Datei / Kommentar | Erledigt |
|---|---|
| `ProcessRequest` ist HTTP-DTO → `api/dto/` | Ja → `api/dto/AiAnalysisRequest`. Für die Fachlogik gibt es den Domain-Typ `domain/AnalyseAuftrag`. Das ungenutzte Feld `prompt` ist entfernt (wird weiter toleriert). |
| `ProcessResponse` ist HTTP-DTO → `api/dto/` | Ja → `api/dto/AcceptedResponse`. |
| `ErgebnisKi`, `Angebotspositionen` als JSON-Schema mit Squad Prozesse/Kunde abstimmen | Schema erstellt (`contracts/ai/AiAnalysisResult.json`); Abstimmung siehe OFFENE-PUNKTE. |
| `ProcessType` Javadoc-Verweis / offene Frage `typ`-Feld | Verweis angepasst; Frage in OFFENE-PUNKTE. |
| Platzhalter `AiAnalysis.java`, `AiAnalysisResult.java` | Gelöscht (ersetzt durch `AnalyseAuftrag` / `ErgebnisKi`). |

## application

| Datei / Kommentar | Erledigt |
|---|---|
| `ProcessTypeDetector` optional als `ProcessType.from(request)` in domain | Ja, als Factory `AnalyseAuftrag.of(...)` in domain. `ProcessTypeDetector` gelöscht. Zusätzlich: fehlender `businessKey` → 400. |
| `MegaLlmService` ist technischer Adapter → infrastructure, Interface `LlmPort` in application | Ja → `application/port/LlmPort` + `infrastructure/llm/MegaLlmAdapter`. Modellwahl/Fallback liegen jetzt im Adapter, Pipeline ist anbieterneutral. |
| `CatalogSearchService` trennen: Port + Rest + Mock, Umschalten per `catalog.mock.enabled` | Ja → `application/port/CatalogSearchPort`, `infrastructure/catalog/RestCatalogSearch`, `MockCatalogSearch`, Auswahl in `CatalogSearchConfiguration`. |
| `Call1PromptBuilder` optional Prompts nach `resources/prompts/` | Ja → `prompts/call1-erstangebot.txt`, `call1-korrektur.txt` (wortgleich), geladen über `PromptTemplates`. |
| `Call2PromptBuilder` optional Parsing trennen + Prompt auslagern | Ja → `Call2AnswerParser`, `prompts/call2-produktauswahl.txt`. |
| `LlmCall1Generator`, `Call2Selector` werden vom Use-Case aufgerufen | Ja. |
| `StubResultGenerator` Packages | Ja; doppelten Code (Erstangebot/Korrektur) zusammengefasst. |
| Platzhalter `AnalyzeVoiceInputUseCase.java` (leer) | Implementiert. |

## infrastructure

| Datei / Kommentar | Erledigt |
|---|---|
| `MegaLlmException` anbieterneutral (`LlmException`) und nach application | Ja → `application/LlmException`. |
| `CatalogCandidate` wird in application genutzt → eigenes Domain-Objekt | Ja → `domain/ProduktKandidat`; `infrastructure/catalog/CatalogCandidateDto` mappt darauf (Preis-Stripping bleibt an der Grenze). |
| `CamundaMessageClient` optional Interface `WorkflowNotifier` davor | Ja → `application/port/WorkflowNotifier` + `infrastructure/processengine/CamundaWorkflowNotifier`. Zusätzlich Retry bei HTTP 400 (Message kam vor der Subscription). |
| `CamundaCorrelationRequest` Message-Name zentral festhalten | Ja → `infrastructure/processengine/WorkflowMessageNames` + `contracts/workflow/ErgebnisKiMessage.json`. |
| `ChatRequest`, `MegaLlmClient` Javadoc-Verweise | Angepasst. |
| `CatalogSearchClient` Vertrag mit Squad Kunde/Auftrag | In OFFENE-PUNKTE. |
| Platzhalter `CatalogClient.java`, `LlmClient.java` | Gelöscht (ersetzt). |
| Platzhalter `SpeechToTextClient.java` (leer) | Gelöscht – Transkription liegt im offer-service. Klärung in OFFENE-PUNKTE. |

## resources / application.properties

| Kommentar | Erledigt |
|---|---|
| 1. Sicherheit (OIDC) | Per Env aktivierbar, lokal aus (siehe oben). |
| 2. Secrets nur als Env-Variable ohne Default | Ja, unverändert. `.env.example` als Vorlage, `.env` in `.gitignore`. |
| 3. `catalog.mock.enabled` Default | Bleibt lokal `true` (catalog-service ist im lokalen Setup optional). Für Online `false` setzen. |
| 4. „Keine eigene DB“ vs. Architektur („AI-service + DB“) | Entscheidung für diesen Sprint: zustandslos. Begründung in OFFENE-PUNKTE. |
| 5. `/mock/catalog-mock.json` vom Classpath | Ja, `src/main/resources/mock/`. |
| Abhängigkeit zu `id-craftvoice.winfprojekt.de` als Default | Entfernt. Default ist das lokale Keycloak (`localhost:8090`). |
| Port 8081 kollidiert mit lokalem user-service | Default jetzt 8086. |

## Weitere bereinigte Teile (Akzeptanzkriterium „unnötiger Code“)

- Verweise auf alte Ticketnummern (#529 – #745) und Personennamen aus Kommentaren entfernt
- `quarkus-rest-client-oidc-filter` ersetzt durch eigenen `TechnicalTokenFilter` (nur aktiv, wenn eingeschaltet → lokal kein Keycloak nötig)
- Native-/Failsafe-Profil aus `pom.xml` entfernt (nicht genutzt)
- `eval/` (Node-Skripte der Modell-Evaluation) nicht übernommen; liegt weiter im Branch `archive/vorsemester`
- Endpoint `POST /ai/analyze` neu: liefert das Ergebnis direkt, damit man lokal ohne Process Engine testen kann
