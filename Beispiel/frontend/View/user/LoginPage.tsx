/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS: frontend/src/features/Login/components/LoginPage.tsx
 * NEUE SCHICHT: View  (ersetzt den leeren Platzhalter View/user/LoginView.tsx)
 * -------------------------------------------------------------------------------------
 * PROBLEM: Die Datei enthält aktuell View UND Logik. Laut MVVM darf die View nur
 * darstellen und Funktionen aus dem ViewModel aufrufen.
 *
 * WAS JETZT ZU TUN IST:
 *
 * 1. Logik nach Beispiel/frontend/ViewModel/user/useLoginViewModel.ts verschieben
 *    (leerer Platzhalter existiert). Konkret dorthin:
 *      - useState: error, isLoading
 *      - 1. useEffect "completeLogin" (initKeycloak() -> getCurrentUser() -> navigate)
 *      - handleLogin() (loginWithKeycloak())
 *      - 2. useEffect (Fehlermeldung nach 8 s ausblenden)
 *      - navigate("/registrieren") / navigate("/passwortVergessen") als Funktionen
 *        goToRegister() / goToPasswordReset()
 *    Der Hook gibt z.B. zurück:
 *      { error, isLoading, login, goToRegister, goToPasswordReset }
 *    Die View ruft nur noch `const vm = useLoginViewModel();` auf und rendert.
 *
 * 2. getErrorMessage() (oben) wird identisch auch in ProfilPage.tsx verwendet
 *    -> einmalig nach Beispiel/frontend/shared/types/ApiError.ts (Platzhalter) oder
 *       shared/utils/ auslagern.
 *
 * 3. Imports anpassen (bewusst noch NICHT geändert):
 *      "../Login.css"            -> "./Login.css"  (liegt jetzt im selben Ordner)
 *      "/src/assets/logos/..."   -> Assets wurden NICHT kopiert, liegen weiter in
 *                                   frontend/src/assets (gemeinsam P + J)
 *      "@/services/authService"  -> infrastructure/auth/authService.ts
 *      "@/services/userService"  -> infrastructure/api/userService.ts
 *      (Nach Schritt 1 braucht nur noch das ViewModel diese Service-Imports.)
 *
 * 4. Route "/login" ist in frontend/src/routes.tsx definiert -> später in
 *    Beispiel/frontend/app/routes.tsx (gemeinsam mit Squad J) eintragen.
 * =====================================================================================
 */
import "../Login.css";
import logo from "/src/assets/logos/CraftVoice_Logo_white_text.png";

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { initKeycloak, loginWithKeycloak } from "@/services/authService";
import { getCurrentUser } from "@/services/userService";

const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Ein unbekannter Fehler ist aufgetreten.";
};

export const LoginPage = () => {
  const navigate = useNavigate();

  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    const completeLogin = async () => {
      try {
        setIsLoading(true);
        setError("");

        /*
         * Keycloak muss zuerst initialisiert werden.
         * Erst danach weiß das Frontend, ob bereits eine gültige Anmeldung besteht.
         */
        const authenticated = await initKeycloak();

        if (!isMounted) {
          return;
        }

        if (!authenticated) {
          setIsLoading(false);
          return;
        }

        /*
         * Nach erfolgreicher Anmeldung wird der User-Service synchronisiert.
         * Dabei wird das aktuelle Keycloak-Zugriffstoken automatisch mitgeschickt.
         */
        await getCurrentUser();

        if (!isMounted) {
          return;
        }

        navigate("/dashboard", {
          replace: true,
        });
      } catch (loginError) {
        if (!isMounted) {
          return;
        }

        console.error("Fehler beim Abschließen der Anmeldung:", loginError);

        setError(
          `Die Anmeldung war erfolgreich, aber das Benutzerprofil konnte nicht synchronisiert werden: ${getErrorMessage(
            loginError,
          )}`,
        );

        setIsLoading(false);
      }
    };

    void completeLogin();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const handleLogin = async () => {
    try {
      setError("");
      setIsLoading(true);

      /*
       * Keycloak übernimmt die Weiterleitung.
       * Danach kommt der Nutzer wieder auf /login zurück.
       */
      await loginWithKeycloak();
    } catch (loginError) {
      console.error("Keycloak-Anmeldung konnte nicht gestartet werden:", loginError);

      setError(
        `Keycloak-Anmeldung konnte nicht gestartet werden: ${getErrorMessage(
          loginError,
        )}`,
      );

      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!error) {
      return;
    }

    const timer = window.setTimeout(() => {
      setError("");
    }, 8000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [error]);

  return (
    <div className="app">
      <div className="card login-card">
        <div className="logo-container">
          <img src={logo} alt="CraftVoice Logo" className="logo" />
        </div>

        <h1>Login</h1>

        <p className="text-secondary">
          Melde dich sicher über Keycloak an.
        </p>

        <div className="divider" />

        {error && (
          <div className="error-banner" role="alert">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <button
          className="button-primary login-btn"
          type="button"
          onClick={handleLogin}
          disabled={isLoading}
        >
          {isLoading
            ? "Anmeldung wird geprüft..."
            : "Mit Keycloak einloggen"}
        </button>

        <button
          className="button-secondary"
          type="button"
          onClick={() => navigate("/registrieren")}
          disabled={isLoading}
        >
          Registrieren
        </button>
        <div className="login-footer">
          <button
            className="text-secondary login-link-button"
            type="button"
            onClick={() => navigate("/passwortVergessen")}
            disabled={isLoading}
          >
            Passwort vergessen?
          </button>
        </div>
      </div>
    </div>
  );
};