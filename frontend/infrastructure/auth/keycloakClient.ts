import Keycloak from "keycloak-js";

function createKeycloak(): Keycloak {
  return new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL ?? "",
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "",
  });
}

// `let` + `export { keycloak as default }` = Live-Binding: Wird die Instanz im Fallback
// ersetzt, sehen alle Importe (apiClient, RequireAuth, ...) automatisch die neue.
let keycloak = createKeycloak();

let initPromise: Promise<boolean> | null = null;

const BASE_INIT_OPTIONS = {
  pkceMethod: "S256",
  checkLoginIframe: false,
  scope: "openid email profile",
} as const;

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
    initPromise = keycloak
      .init({
        ...BASE_INIT_OPTIONS,
        onLoad: "check-sso",
        // Prüft die Anmeldung in einem unsichtbaren iframe statt die ganze Seite
        // zu Keycloak umzuleiten. Die Startseite erscheint dadurch immer sofort,
        // auch wenn Keycloak die Redirect-URI ablehnt.
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        // keycloak-js-Standard. Kürzer schlägt fehl, wenn Keycloak z. B. direkt nach einem
        // Redeploy langsam antwortet ("Timeout when waiting for 3rd party check iframe message").
        messageReceiveTimeout: 10000,
      })
      .catch((silentError: unknown) => {
        // Die stille Prüfung per iframe ist fehlgeschlagen (Keycloak langsam, Browser oder
        // Erweiterung blockiert Drittanbieter-iframes). Eine Keycloak-Instanz lässt sich nur
        // einmal initialisieren, daher neu anlegen und ohne iframe-Prüfung starten: Ein
        // Login-Rückruf in der URL wird trotzdem verarbeitet, sonst gilt der Nutzer als
        // nicht angemeldet und keycloak.login() leitet normal zu Keycloak weiter.
        console.warn("Stille Keycloak-Prüfung fehlgeschlagen, starte ohne iframe:", silentError);
        keycloak = createKeycloak();
        return keycloak.init(BASE_INIT_OPTIONS);
      })
      .catch((error: unknown) => {
        // Nicht dauerhaft im Fehlerzustand bleiben: der nächste Aufruf versucht es erneut
        // (mit frischer Instanz, die alte ist bereits initialisiert).
        keycloak = createKeycloak();
        initPromise = null;
        throw error;
      });
  }
  return initPromise;
}

export { keycloak as default };
