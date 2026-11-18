import { Link } from "react-router-dom";
import { AuthLayout } from "../shared/WerkmundoShell";
import { useAuthViewModel } from "../../ViewModel/user/useAuthViewModel";

export function LoginView() {
  const { error, isLoading, isSubmitting, login } = useAuthViewModel();

  return (
    <AuthLayout title="Login">
      <h1 className="wm-auth-title">
        Willkommen
        <span>zurück.</span>
      </h1>
      {isLoading ? (
        <p className="wm-dashboard-feedback">Anmeldung wird geprüft …</p>
      ) : (
        <form
          className="wm-auth-actions"
          onSubmit={(event) => {
            event.preventDefault();
            void login();
          }}
        >
          <p>Die Anmeldung erfolgt über Keycloak.</p>
          {error && <p className="wm-error" role="alert">{error}</p>}
          <div className="wm-button-row">
            <button className="wm-button wm-button-primary" type="submit" disabled={isSubmitting}>
              Anmelden
            </button>
            <Link className="wm-button wm-button-secondary" to="/registrieren">
              Konto erstellen
            </Link>
          </div>
        </form>
      )}
    </AuthLayout>
  );
}
