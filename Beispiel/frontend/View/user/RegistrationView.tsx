import { useState } from "react";
import { Link } from "react-router-dom";
import { AuthLayout } from "../shared/WerkmundoShell";
import { useAuthViewModel } from "../../ViewModel/user/useAuthViewModel";

export function RegistrationView() {
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false);
  const { error, isLoading, isSubmitting, register } = useAuthViewModel();

  return (
    <AuthLayout title="Registrieren">
      <h1 className="wm-auth-title">
        Weniger tippen.
        <span>Mehr schaffen.</span>
      </h1>
      <p className="wm-auth-description">
        Mach aus gesprochenen Aufträgen übersichtliche Angebotsentwürfe.
        <br />
        Starte jetzt mit deinem Konto.
      </p>
      <form
        className="wm-auth-actions wm-register-actions"
        onSubmit={(event) => {
          event.preventDefault();
          void register(acceptedPrivacy);
        }}
      >
        <p>Erstelle hier deinen Account</p>
        <label className="wm-consent">
          <input
            type="checkbox"
            checked={acceptedPrivacy}
            onChange={(event) => setAcceptedPrivacy(event.target.checked)}
          />
          <span>
            Ich stimme den <Link to="/datenschutz">Datenschutzbedingungen</Link> zu.
          </span>
        </label>
        {error && <p className="wm-error" role="alert">{error}</p>}
        <div className="wm-button-row">
          <button
            className="wm-button wm-button-primary"
            type="submit"
            disabled={isLoading || isSubmitting}
          >
            {isSubmitting ? "Weiterleitung läuft …" : "Registrieren"}
          </button>
          <Link className="wm-button wm-button-secondary" to="/login">
            Schon ein Konto?
          </Link>
        </div>
      </form>
    </AuthLayout>
  );
}
