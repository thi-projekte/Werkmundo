import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "../shared/WerkmundoShell";

export function LoginView() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");

  return (
    <AuthLayout title="Login">
      <h1 className="wm-auth-title">
        Willkommen
        <span>zurück.</span>
      </h1>
      <form
        className="wm-auth-actions"
        onSubmit={(event) => {
          event.preventDefault();
          navigate("/dashboard");
        }}
      >
        <label>
          E-Mail-Adresse
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="du@deinefirma.de"
          />
        </label>
        <label>
          Passwort
          <input type="password" required minLength={4} placeholder="Passwort" />
        </label>
        <div className="wm-button-row">
          <button className="wm-button wm-button-primary" type="submit">
            Anmelden
          </button>
          <Link className="wm-button wm-button-secondary" to="/registrieren">
            Konto erstellen
          </Link>
        </div>
      </form>
    </AuthLayout>
  );
}