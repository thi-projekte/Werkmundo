/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS: frontend/src/services/authService.ts
 * NEUE SCHICHT: infrastructure/auth  (ersetzt zusammen mit keycloak.ts den leeren
 *               Platzhalter infrastructure/auth/keycloakClient.ts)
 * -------------------------------------------------------------------------------------
 * WICHTIG: Wird von ALLEN API-Dateien (auch von Squad J: offerService, documentApi, ...)
 * für getToken() genutzt -> Änderungen an der Schnittstelle mit Squad J abstimmen.
 *
 * WAS JETZT ZU TUN IST:
 *
 * 1. Diese Datei soll die EINZIGE Stelle sein, die das Keycloak-Objekt kennt. Dafür
 *    ergänzen:
 *      - registerWithKeycloak()   (wird in View/user/RegistrierungPage.tsx direkt
 *                                  per keycloak.register() gemacht)
 *      - getCurrentUserEmail()    (wird in View/user/PasswordChangePage.tsx direkt
 *                                  aus keycloak.tokenParsed gelesen)
 *    und danach die direkten "@/core/keycloak"-Imports in den Views entfernen.
 *
 * 2. getToken() wird später vom gemeinsamen Beispiel/frontend/infrastructure/http/apiClient.ts
 *    (Platzhalter) aufgerufen, damit nicht jede API-Datei den Header selbst baut.
 *
 * 3. Import anpassen (bewusst noch NICHT geändert):
 *      "@/core/keycloak" -> "./keycloak" (liegt jetzt im selben Ordner)
 * =====================================================================================
 */
import keycloak from "@/core/keycloak";

let keycloakInitPromise: Promise<boolean> | null = null;

export const initKeycloak = (): Promise<boolean> => {
  if (!keycloakInitPromise) {
    keycloakInitPromise = keycloak
      .init({
        onLoad: "check-sso",
        pkceMethod: "S256",
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      .catch((error: unknown) => {
        /*
         * Falls die Initialisierung fehlschlägt, darf ein späterer
         * erneuter Versuch möglich bleiben.
         */
        keycloakInitPromise = null;
        throw error;
      });
  }

  return keycloakInitPromise;
};

export const loginWithKeycloak = async (): Promise<void> => {
  const authenticated = await initKeycloak();

  /*
   * Falls bereits eine gültige Keycloak-Anmeldung besteht,
   * muss kein zweiter Login gestartet werden.
   */
  if (authenticated) {
    return;
  }

  await keycloak.login({
    redirectUri: `${window.location.origin}/login`,
  });
};

export const logoutFromKeycloak = async (): Promise<void> => {
  await initKeycloak();

  await keycloak.logout({
    redirectUri: `${window.location.origin}/login`,
  });
};

export const openKeycloakAccountConsole = async (): Promise<void> => {
  const authenticated = await initKeycloak();

  if (!authenticated) {
    await keycloak.login({
      redirectUri: `${window.location.origin}/profil`,
    });
    return;
  }

  window.location.href = keycloak.createAccountUrl();
};

export const isAuthenticated = (): boolean => {
  return Boolean(keycloak.authenticated);
};

export const getToken = async (): Promise<string | null> => {
  const authenticated = await initKeycloak();

  if (!authenticated) {
    return null;
  }

  await keycloak.updateToken(30);

  return keycloak.token ?? null;
};

export default keycloak;