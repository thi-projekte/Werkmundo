import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? "",
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "",
});

let initPromise: Promise<boolean> | null = null;

export function isKeycloakConfigured(): boolean {
  return Boolean(
    import.meta.env.VITE_KEYCLOAK_URL &&
      import.meta.env.VITE_KEYCLOAK_REALM &&
      import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
  );
}

export function initKeycloak(): Promise<boolean> {
  if (!isKeycloakConfigured()) {
    return Promise.resolve(false);
  }
  if (!initPromise) {
    initPromise = keycloak.init({
      onLoad: "check-sso",
      // Prüft die Anmeldung in einem unsichtbaren iframe statt die ganze Seite
      // zu Keycloak umzuleiten. Die Startseite erscheint dadurch immer sofort,
      // auch wenn Keycloak die Redirect-URI ablehnt.
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      messageReceiveTimeout: 5000,
      pkceMethod: "S256",
      checkLoginIframe: false,
      scope: "openid email profile",
    });
  }
  return initPromise;
}

export default keycloak;
