# Werkmundo online (Server winfprojekt.de)

Server: `162.55.57.51`, Portainer: https://portainer.winfprojekt.de, Nginx Proxy Manager: http://winfprojekt.de:81

## Aufbau

```
Browser ──https──> Nginx Proxy Manager ──> werkmundo-frontend:80 (Nginx)
                                             ├─ /               React-App
                                             ├─ /svc/user/      -> werkmundo-user:8080
                                             ├─ /svc/offer/     -> werkmundo-offer:8080
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
     `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_DB_PASSWORD` (Werte nur hier, nie ins Git).
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

## Noch nicht im Stack

- **Process Engine** (Camunda): ohne sie schlägt „Angebot erstellen“ fehl. Sobald es ein Image gibt,
  als Service `werkmundo-process-engine` ergänzen oder `PE_URL` setzen.
- **Catalog-Service** und **ai-service**: analog über `CATALOG_SERVICE_URL` bzw. einen eigenen Service.
