import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import keycloak, { initKeycloak, isKeycloakConfigured } from "../../infrastructure/auth/keycloakClient";

const errorMessage = (error: unknown) =>
  error instanceof Error ? error.message : "Ein unbekannter Fehler ist aufgetreten.";

export function useAuthViewModel() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isKeycloakConfigured()) {
      setError("SSO ist derzeit nicht konfiguriert. Bitte wende dich an den Support.");
      setIsLoading(false);
      return;
    }

    let active = true;

    const checkAuthentication = async () => {
      try {
        const authenticated = await initKeycloak();
        if (!active) return;

        if (authenticated) {
          navigate("/dashboard", { replace: true });
        } else {
          setIsLoading(false);
        }
      } catch (authError) {
        if (!active) return;
        console.error("Die Anmeldung konnte nicht geprüft werden:", authError);
        setError(`Die Anmeldung konnte nicht geprüft werden: ${errorMessage(authError)}`);
        setIsLoading(false);
      }
    };

    void checkAuthentication();
    return () => {
      active = false;
    };
  }, [navigate]);

  const login = async () => {
    if (!isKeycloakConfigured()) {
      setError("SSO ist derzeit nicht konfiguriert. Bitte wende dich an den Support.");
      return;
    }

    setError("");
    setIsSubmitting(true);
    try {
      await initKeycloak();
      await keycloak.login({
        redirectUri: `${window.location.origin}/dashboard`,
      });
    } catch (authError) {
      console.error("Keycloak-Anmeldung konnte nicht gestartet werden:", authError);
      setError(`Anmeldung fehlgeschlagen: ${errorMessage(authError)}`);
      setIsSubmitting(false);
    }
  };

  const register = async (acceptedPrivacy: boolean) => {
    if (!acceptedPrivacy) {
      setError("Bitte stimme den Datenschutzbedingungen zu.");
      return;
    }

    if (!isKeycloakConfigured()) {
      setError("Die Registrierung ist derzeit nicht verfügbar. Bitte wende dich an den Support.");
      return;
    }

    setError("");
    setIsSubmitting(true);
    try {
      await initKeycloak();
      await keycloak.register({
        redirectUri: `${window.location.origin}/einrichtung/unternehmen`,
      });
    } catch (authError) {
      console.error("Keycloak-Registrierung konnte nicht gestartet werden:", authError);
      setError(`Registrierung fehlgeschlagen: ${errorMessage(authError)}`);
      setIsSubmitting(false);
    }
  };

  return { error, isLoading, isSubmitting, login, register };
}
