# Werkmundo online (Server winfprojekt.de)

Server: `162.55.57.51`, Portainer: https://portainer.winfprojekt.de, Nginx Proxy Manager: http://winfprojekt.de:81

## Aufbau

```
Browser ──https──> Nginx Proxy Manager ──> werkmundo-frontend:80 (Nginx)
                                             ├─ /               React-App
                                             ├─ /svc/user/      -> werkmundo-user:8080
                                             ├─ /svc/offer/     -> werkmundo-offer:8080 ──> werkmundo-ai:8086 ──> MegaLLM
                                             └─ /svc/invoice/   -> werkmundo-invoice:8080
Browser ──https──> Nginx Proxy Manager ──> werkmundo-keycloak:8080 (Login, Realm werkmundo)
```

Nur `werkmundo-frontend` und `werkmundo-keycloak` hängen am Docker-Netzwerk `nginx-proxy-manager`. Services und
Datenbanken liegen im Netzwerk `werkmundo-intern` und haben keine Ports nach außen.
Weil alles unter einer Domain läuft, braucht es kein CORS und nur ein Zertifikat.

| Datei | Zweck |
|---|---|
| `deploy/docker-compose.yml` | Portainer-Stack |
| `deploy/Dockerfile.frontend` | Frontend-Image mit relativen API-URLs |
| `deploy/nginx.conf` | Nginx im Frontend-Container (SPA und `/svc/*`-Weiterleitung) |
| `deploy/keycloak/` | eigenes Keycloak-Image mit Realm `werkmundo` (`werkmundo-realm.json`) |
| `.github/workflows/deploy-images.yml` | baut die Images nach `ghcr.io/thi-projekte/werkmundo-*` |

## Einmalige Einrichtung

1. **Images bauen:** Merge nach `main` (oder Actions → *Deploy-Images* → *Run workflow*).
   Danach in GitHub unter *Packages* jedes `werkmundo-*`-Paket auf **Public** stellen,
   oder in Portainer unter *Registries* ghcr.io mit einem Token (`read:packages`) eintragen.
2. **Keycloak** läuft im eigenen Stack mit. Realm `werkmundo`, Rollen `OWNER`/`EMPLOYEE`/`ACCOUNTANT`
   und der Client `werkmundo-frontend` (inkl. Redirect-URIs für `werkmundo.winfprojekt.de`,
   `werkmundo.de` und `localhost:5173`) werden beim ersten Start aus `deploy/keycloak/werkmundo-realm.json`
   importiert. Admin-Konsole: https://werkmundo-keycloak.winfprojekt.de/admin (Login mit
   `KEYCLOAK_ADMIN_USER`/`KEYCLOAK_ADMIN_PASSWORD`).
   Der Import passiert nur, solange der Realm noch nicht existiert. Spätere Änderungen in der
   Admin-Konsole machen und zusätzlich in `werkmundo-realm.json` nachziehen.
3. **Portainer** → *Stacks* → *Add stack* → Name `werkmundo` → *Repository*:
   - URL `https://github.com/thi-projekte/Werkmundo`, Reference `refs/heads/main`, Compose path `deploy/docker-compose.yml`
   - Environment variables: `USER_DB_PASSWORD`, `OFFER_DB_PASSWORD`, `INVOICE_DB_PASSWORD`,
     `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_DB_PASSWORD`, `MEGALLM_API_KEY`
     (Werte nur hier, nie ins Git).
     Die Keycloak-Admin-Daten werden nur beim allerersten Start übernommen.
   - Optional *GitOps updates* aktivieren, damit Änderungen an der Compose-Datei automatisch kommen.
4. **Nginx Proxy Manager** → *Add Proxy Host*:
   - Domain `werkmundo.winfprojekt.de` (die Wildcard `*.winfprojekt.de` zeigt schon auf den Server)
   - Scheme `http`, Forward Hostname `werkmundo-frontend`, Port `80`, *Block Common Exploits* an
   - SSL: *Request a new certificate*, *Force SSL*, *HTTP/2*
   - Zweiter Proxy Host: `werkmundo-keycloak.winfprojekt.de` → `http` `werkmundo-keycloak` Port `8080`,
     gleiche SSL-Einstellungen
5. Testen: https://werkmundo.winfprojekt.de öffnen, einloggen, Dashboard laden.

## Update ausrollen

Push nach `main` → **warten, bis der Workflow *Deploy-Images* grün ist** → in Portainer im Stack
*Pull and redeploy* (bzw. *Update the stack* mit *Re-pull image*). Wer vorher redeployt, bekommt
noch die alten Images. Für einen festen Stand `IMAGE_TAG=sha-<commit>` setzen.

## KI (ai-service)

"Angebot erstellen" läuft online **ohne Process Engine** (`AI_DIRECT_ENABLED=true`, Standard im Stack):

1. Frontend → `POST /svc/offer/offers` mit Sprachschnipsel (über `werkmundo-gateway` an den offer-service)
2. offer-service → `POST http://werkmundo-ai:8086/ai/analyze` (synchron, max. 50 s)
3. ai-service → MegaLLM (`MEGALLM_API_KEY`): Positionen, Material, geschätzte Arbeitsdauer
4. offer-service speichert das Angebot als `ENTWURF` mit den KI-Positionen (ohne Preise, Arbeitszeit
   mit Stundensatz aus dem user-service). Der Handwerker bepreist und speichert sie in der Angebotsbearbeitung.

Antwortet die KI nicht (Key fehlt, Timeout, Fehler), wird das Angebot trotzdem als leerer `ENTWURF` angelegt.
Ohne `MEGALLM_API_KEY` liefert der ai-service nur ein festes Beispiel (Wallbox).

Der ai-service hängt nur im Netz `werkmundo-intern` und braucht deshalb keinen Proxy Host und kein Token.
Die Katalogsuche nutzt den eingebauten Mock-Katalog, die Katalog-IDs werden im Direktmodus nicht übernommen.

**Spracheingabe:** Die Transkription (`/speech-capture/transcribe`) braucht einen Deepgram-Key
(`DEEPGRAM_API_KEY` am offer-service). Ohne ihn funktioniert nur die Texteingabe.

## Noch nicht im Stack

- **Process Engine** (Camunda): wird für "Angebot erstellen" nicht mehr gebraucht (Direktmodus), aber für
  den restlichen Prozess (Korrektur per Sprache, Versand). Sobald es ein Image gibt, als Service
  `werkmundo-process-engine` ergänzen und `AI_DIRECT_ENABLED=false` setzen.
