/*
 * [Squad P] KOPIERT AUS: frontend/src/core/keycloak.ts
 * NEUE SCHICHT: infrastructure/auth  (Keycloak-Instanz; zusammen mit authService.ts Ersatz
 *               für den leeren Platzhalter infrastructure/auth/keycloakClient.ts)
 * TODO:
 *   1. Nur authService.ts darf diese Datei importieren. Aktuell importieren auch
 *      View/user/RegistrierungPage.tsx und View/user/PasswordChangePage.tsx das Objekt
 *      direkt -> siehe TODO in authService.ts Punkt 1.
 *   2. Env-Variablen VITE_KEYCLOAK_URL / _REALM / _CLIENT_ID kommen aus der Vite-Konfiguration
 *      des Original-Frontends (frontend/.env bzw. docker-compose) - beim Zusammenführen
 *      mitübernehmen. Optional: Fehlermeldung, falls eine Variable fehlt.
 */
import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

export default keycloak;