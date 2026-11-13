import { Link } from "react-router-dom";
import { AuthLayout } from "../shared/WerkmundoShell";

export function LogoutView() {
  return (
    <AuthLayout title="Abmelden">
      <h1 className="wm-auth-title">
        Schade, dass du
        <span>schon gehst.</span>
      </h1>
      <p className="wm-auth-description">
        Deine Angebote warten schon.<br />
        Meld dich wieder an und mach genau da weiter, wo du aufgehört hast.
      </p>
      <div className="wm-auth-actions">
        <p>Melde dich hier mit deinem Account an</p>
        <div className="wm-button-row">
          <Link className="wm-button wm-button-primary" to="/login">SSO Login</Link>
          <Link className="wm-button wm-button-secondary" to="/registrieren">Noch kein Konto?</Link>
        </div>
      </div>
    </AuthLayout>
  );
}
