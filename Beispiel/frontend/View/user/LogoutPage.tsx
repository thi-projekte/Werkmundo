/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS: frontend/src/features/Logout/components/LogoutPage.tsx
 * NEUE SCHICHT: View  (reine Darstellung, enthält bereits keine Logik)
 * -------------------------------------------------------------------------------------
 * WAS JETZT ZU TUN IST:
 *
 * 1. Der Button "Erneut einloggen" hat KEINEN onClick -> funktioniert nicht.
 *    -> Kleine Funktion goToLogin() (navigate("/login")) ergänzen, z.B. in einem
 *       ViewModel/user/useLogoutViewModel.ts oder mit in useLoginViewModel.ts.
 *
 * 2. OFFEN / prüfen: logoutFromKeycloak() (infrastructure/auth/authService.ts) leitet nach
 *    dem Logout direkt auf /login um, nicht auf /logout. Diese Seite wird daher aktuell
 *    vermutlich nie angezeigt. -> Entweder redirectUri auf /logout ändern oder Seite löschen.
 *
 * 3. Import "@/assets/stylesheets/stylesheet.css" ist das globale Stylesheet (nicht kopiert,
 *    liegt in frontend/src/assets) -> später zentral in main.tsx/App laden statt pro View.
 * =====================================================================================
 */
import "@/assets/stylesheets/stylesheet.css";

export const LogoutPage = () => {
  return (
    <div className="app">
      <div className="card logout-card">
        <h1>Abgemeldet</h1>
        <p className="text-secondary">
          Du wurdest erfolgreich ausgeloggt.
        </p>

        <div className="divider"></div>

        <button className="button-primary">
          Erneut einloggen
        </button>
      </div>

    </div>
  );
};