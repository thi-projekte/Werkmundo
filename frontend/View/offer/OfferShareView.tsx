import { Link } from "react-router-dom";
import { logoutFromKeycloak } from "../../infrastructure/auth/keycloakClient";
import { AuthLayout } from "../shared/WerkmundoShell";

export function OfferShareView() {
  return (
    <AuthLayout title="Dein Angebot" backTo="/dashboard">
      <h1 className="wm-auth-title">
        Dein Angebot
        <span>wurde verschickt.</span>
      </h1>
      <p className="wm-auth-description">Jetzt warten wir nur noch auf die Rückmeldung von deinem Kunden.</p>
      <div className="wm-auth-actions">
        <div className="wm-button-row">
          <Link className="wm-button wm-button-primary" to="/dashboard">Zurück zur Startseite</Link>
          <button
            className="wm-button wm-button-secondary"
            type="button"
            onClick={() => {
              void logoutFromKeycloak().catch((error: unknown) => {
                console.error("Keycloak-Abmeldung konnte nicht gestartet werden:", error);
              });
            }}
          >
            Abmelden
          </button>
        </div>
      </div>
    </AuthLayout>
  );
}