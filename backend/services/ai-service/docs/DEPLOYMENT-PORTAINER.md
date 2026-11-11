# ai-service auf dem Server einrichten (Portainer + Nginx Proxy Manager)

Ziel: `https://aiservice-craftvoice.winfprojekt.de` zeigt auf den Container `ai-service:8081`.

```text
Browser ──HTTPS──▶ Nginx Proxy Manager ──▶ ai-service:8081   (Netz: nginx-proxy-manager)
                                              ▲      │
             Process Engine ──POST /ai/process┘      └──▶ MegaLLM, Deepgram, catalog-service, Keycloak
```

## 0. Vorher besorgen

| Was | Woher |
|---|---|
| `MEGALLM_API_KEY` | Alex Grebe (wie im Vorsemester) |
| `DEEPGRAM_API_KEY` | aus dem bisherigen offer-service-Stack in Portainer (Variable `DEEPGRAM_API_KEY`) oder neuer Key auf console.deepgram.com |
| `AI_SERVICE_OIDC_SECRET` | Keycloak-Admin → Realm `handwerker-realm` → Clients → `ai-service` → Tab *Credentials* → *Client secret* |

Gibt es den Client `ai-service` in Keycloak nicht: *Create client* → Client-ID `ai-service`,
*Client authentication* = On, *Service accounts roles* = On, *Standard flow* = Off.

## 1. Alten Container prüfen

In Portainer → *Containers*: Läuft noch ein `ai-service` vom Vorsemester (Image
`ghcr.io/thi-projekte/handwerker/ai-service`)? Dann dessen Stack unter *Stacks* stoppen bzw. löschen,
sonst kollidiert der Container-Name. Vorher die Werte der Stack-Variablen notieren
(*Stacks → Stack → Editor*, unten *Environment variables*) – die Secrets lassen sich wiederverwenden.

## 2. Stack anlegen

Portainer → *Stacks* → *Add stack*:

1. **Name:** `werkmundo-ai-service`
2. **Build method:** *Repository*
3. **Repository URL:** `https://github.com/thi-projekte/Werkmundo`
   - Repository privat? → *Authentication* an, GitHub-Benutzer + Personal Access Token (nur `repo`-Leserecht).
4. **Repository reference:** `refs/heads/main` (zum Testen vorher den Feature-Branch)
5. **Compose path:** `backend/services/ai-service/docker-compose.portainer.yml`
6. **Environment variables** (*Add an environment variable*):

   | Name | Wert |
   |---|---|
   | `MEGALLM_API_KEY` | *(Secret)* |
   | `DEEPGRAM_API_KEY` | *(Secret)* |
   | `AI_SERVICE_OIDC_SECRET` | *(Secret)* |
   | `CAMUNDA_ENGINE_URL` | gleicher Wert wie `PE_URL` im offer-service-Stack, z.B. `http://craftvoice-processengine:8080/engine-rest` |
   | `AI_CORS_ORIGINS` | Domain(s) des Frontends, kommagetrennt, z.B. `https://craftvoice.winfprojekt.de` |

   Optional (Defaults passen meistens): `CATALOG_SERVICE_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM`.
7. *Deploy the stack*. Der erste Build dauert ein paar Minuten (Maven lädt Abhängigkeiten).

Updates nach einem Merge: *Stacks → werkmundo-ai-service → Pull and redeploy* (Haken bei
*Re-pull image and redeploy*). Alternativ unter *GitOps updates* automatisches Polling aktivieren.

**Wichtig:** Process Engine und catalog-service müssen im selben Docker-Netz hängen, damit
`craftvoice-processengine` bzw. `catalog-service` auflösbar sind. Prüfen unter
*Networks → nginx-proxy-manager → Containers*. Hängen sie in einem anderen Netz, dieses Netz in
`docker-compose.portainer.yml` zusätzlich als `external` eintragen.

## 3. Nginx Proxy Manager

*Hosts → Proxy Hosts*: Gibt es `aiservice-craftvoice.winfprojekt.de` schon, nur prüfen. Sonst *Add Proxy Host*:

| Feld | Wert |
|---|---|
| Domain Names | `aiservice-craftvoice.winfprojekt.de` |
| Scheme | `http` |
| Forward Hostname / IP | `ai-service` |
| Forward Port | `8081` |
| Block Common Exploits | an |
| Websockets Support | aus |
| SSL-Tab | Let's-Encrypt-Zertifikat, *Force SSL* an |

Sprachaufnahmen sind größer als Nginx' Standardlimit von 1 MB. Im Tab **Advanced** eintragen:

```nginx
client_max_body_size 25m;
proxy_read_timeout 120s;
```

## 4. Testen

```bash
curl https://aiservice-craftvoice.winfprojekt.de/q/health
```

→ `"status": "UP"`. Danach eine Testdatei transkribieren:

```bash
curl -X POST -F "audio=@aufnahme.webm;type=audio/webm" https://aiservice-craftvoice.winfprojekt.de/ai/transcribe
```

Fehlerbilder:

| Symptom | Ursache |
|---|---|
| 502 Bad Gateway von Nginx | Container läuft nicht oder ist nicht im Netz `nginx-proxy-manager` |
| 413 Request Entity Too Large | `client_max_body_size` fehlt (Schritt 3) |
| Browser: CORS-Fehler | Frontend-Domain fehlt in `AI_CORS_ORIGINS` |
| Antwort ist immer der Wallbox-Beispieltext | `DEEPGRAM_API_KEY` nicht gesetzt (Stub) |
| `{"error":"Die Spracherkennung ist gerade nicht erreichbar…"}` | Deepgram-Key ungültig → Container-Log in Portainer prüfen |
| Angebot hängt nach `/ai/process` | `CAMUNDA_ENGINE_URL` falsch oder `AI_SERVICE_OIDC_SECRET` falsch → Log zeigt den Fehler |

## 5. Frontend

Beim Frontend-Build `VITE_AI_SERVICE_URL=https://aiservice-craftvoice.winfprojekt.de` setzen.
