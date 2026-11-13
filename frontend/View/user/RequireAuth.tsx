import { useEffect, useState, type ReactNode } from "react";
import keycloak, { initKeycloak, isKeycloakConfigured } from "../../infrastructure/auth/keycloakClient";

type RequireAuthProps = {
  children: ReactNode;
};

export function RequireAuth({ children }: RequireAuthProps) {
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isKeycloakConfigured()) {
      setError("SSO ist derzeit nicht konfiguriert. Bitte wende dich an den Support.");
      return;
    }

    let active = true;

    void initKeycloak()
      .then((authenticated) => {
        if (!active) return;
        if (!authenticated) {
          return keycloak.login({ redirectUri: window.location.href });
        }
        setReady(true);
      })
      .catch((authError: unknown) => {
        if (!active) return;
        const message = authError instanceof Error ? authError.message : "Unbekannter Fehler";
        setError(`Die Anmeldung konnte nicht geprüft werden: ${message}`);
      });

    return () => {
      active = false;
    };
  }, []);

  if (error) {
    return <p className="wm-error" role="alert">{error}</p>;
  }

  if (!ready) {
    return <p className="wm-dashboard-feedback">Anmeldung wird geprüft …</p>;
  }

  return children;
}
