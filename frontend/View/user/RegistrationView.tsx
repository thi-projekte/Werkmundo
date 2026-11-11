import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "../shared/WerkmundoShell";

export function RegistrationView() {
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  return (
    <AuthLayout title="Registrieren">
      <h1 className="wm-auth-title">
        Weniger tippen.
        <span>Mehr schaffen.</span>
      </h1>
      <form
        className="wm-auth-actions wm-register-actions"
        onSubmit={(event) => {
          event.preventDefault();
          if (!acceptedPrivacy) {
            setError("Bitte stimme den Datenschutzbedingungen zu.");
            return;
          }
          setError("");
          navigate("/einrichtung/unternehmen");
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
          <button className="wm-button wm-button-primary" type="submit">
            Registrieren
          </button>
          <Link className="wm-button wm-button-secondary" to="/login">
            Schon ein Konto?
          </Link>
        </div>
      </form>
    </AuthLayout>
  );
}
