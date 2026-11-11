# Offene Punkte und Abhängigkeiten zu anderen Squads

Squads laut `Beispiel/docs/team-ownership.md`:
**Team 1 Handwerker** (ai-service, wir) · **Team 2 Kunde/Auftrag** (offer-, catalog-, document-service) · **Team 3 Prozesse** (Process Engine, BPMN, `contracts/workflow`).

## Mit Team 3 (Prozesse) abstimmen

1. **Request-Vertrag** `contracts/ai/AiAnalysisRequest.json` bestätigen. Feldnamen sind unverändert zum Vorsemester (`businessKey`, `vorlage`, `sprachschnipsel`, `strukturierteAngebotspositionen`, `korrekturschnipsel`).
2. **Explizites `typ`-Feld?** Derzeit wird Erstangebot/Korrektur aus den vorhandenen Feldern abgeleitet. Ein Feld `typ: "ERSTANGEBOT" | "KORREKTUR"` wäre robuster.
3. **Message `ergebnisKI`** (`contracts/workflow/ErgebnisKiMessage.json`): Name, Variable als `String`-Typ, Korrelation per `businessKey`. Gehört nach `Beispiel/contracts/workflow/` und `processengine/integration/WorkflowMessageNames.java`.
4. **URL im BPMN:** HTTP-Connector muss auf `http://<ai-service-host>:8086/ai/process` zeigen (Vorsemester: Port 8081).
5. **Auth (späterer Sprint):** Mit welchem Token ruft die Engine auf (Rolle `process-engine`?), und akzeptiert `/engine-rest/message` das technische Token des Clients `ai-service`?
6. **Header `X-Handwerker-Id`** weiter von der Engine mitschicken (Mandant für die Katalogsuche).

## Mit Team 2 (Kunde/Auftrag) abstimmen

1. **Such-Endpoint des catalog-service:** `GET /catalog/material/search?q=&limit=` + Header `X-Handwerker-Id`, Antwort `{ "candidates": [ {id, articleNumber, name, description, unit, category} ] }`. Bleibt der Vertrag so?
2. **`katalogProduktId`** (String/UUID) ist die Verknüpfung für den Preis-Lookup im offer-service. Mit dem Mock-Katalog sind die IDs Zahlen → keine Preise. Für einen Ende-zu-Ende-Test daher `CATALOG_MOCK_ENABLED=false` und lokalen catalog-service nutzen.
3. **Speech-to-Text:** ✅ (Issue #55) Liegt jetzt im ai-service (`POST /ai/transcribe`, Deepgram), da „Sprachaufnahme und KI-Analyse“ laut Team-Ownership zu Team 1 gehört. Das Frontend nutzt nur noch diesen Endpoint. Mit Team 2 abstimmen, ob `/speech-capture/transcribe` im offer-service entfernt wird.
4. **`leistungen`** werden laut altem Datenvertrag vom offer-service beim Speichern ignoriert – prüfen.

## Im eigenen Team entscheiden

1. **Eigene Datenbank?** Die Architektur zeigt „AI-service + DB“. Für die aktuelle Funktion ist keine nötig (zustandslos). Sinnvoll wäre eine DB erst für Verlauf/Logging der KI-Ergebnisse oder Kosten-Monitoring (Token-Zahlen liegen in `ChatResponse.usage`). Vorschlag: im späteren Sprint, zusammen mit dem Online-Betrieb.
2. **Keycloak-Realm:** Der lokale Realm `local-setup/keycloak/handwerker-realm.json` hat noch keinen Client `ai-service` und keine Rolle `process-engine`. Nötig erst für den Online-Betrieb.
3. **Einbindung in das lokale Gesamt-Setup** (Issue #40): Vorschlag in [LOKALES-SETUP.md](LOKALES-SETUP.md).
4. **Prompts:** Änderungen an `src/main/resources/prompts/*.txt` bitte neu evaluieren (die Call-1-Prompts sind wortgleich zur Modellwahl-Evaluation).
