import { Link, Navigate, Route, Routes } from "react-router-dom";
import { AuthLayout } from "./View/shared/WerkmundoShell";
import { LoginView } from "./View/user/LoginView";
import { RegistrationView } from "./View/user/RegistrationView";
import { ContactView } from "./View/legal/ContactView";
import { ImprintView } from "./View/legal/ImprintView";
import { PrivacyView } from "./View/legal/PrivacyView";
import { DashboardView } from "./View/dashboard/DashboardView";
import { CompanySetupView } from "./View/onboarding/CompanySetupView";
import { InvoiceSetupView } from "./View/onboarding/InvoiceSetupView";
import { VoiceInputView } from "./View/voice/VoiceInputView";

function HomeView() {
  return (
    <AuthLayout title="Willkommen">
      <h1 className="wm-auth-title">
        Weniger tippen.
        <span>Mehr schaffen.</span>
      </h1>
      <div className="wm-button-row">
        <Link className="wm-button wm-button-primary" to="/login">
          Zur Anmeldung
        </Link>
        <Link className="wm-button wm-button-secondary" to="/registrieren">
          Registrieren
        </Link>
      </div>
    </AuthLayout>
  );
}

function NotFoundView() {
  return (
    <AuthLayout title="Seite nicht gefunden">
      <h1 className="wm-auth-title">Diese Seite gibt es nicht.</h1>
      <Link className="wm-button wm-button-primary" to="/">
        Zur Startseite
      </Link>
    </AuthLayout>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeView />} />
      <Route path="/login" element={<LoginView />} />
      <Route path="/registrieren" element={<RegistrationView />} />
      <Route path="/dashboard" element={<DashboardView />} />
      <Route path="/aufnahme" element={<VoiceInputView />} />
      {/* Alter Pfad aus dem Vorsemester (Navbar „Neues Angebot“) */}
      <Route path="/home" element={<Navigate to="/aufnahme" replace />} />
      <Route path="/einrichtung/unternehmen" element={<CompanySetupView />} />
      <Route path="/einrichtung/rechnungen" element={<InvoiceSetupView />} />
      <Route path="/kontakt" element={<ContactView />} />
      <Route path="/impressum" element={<ImprintView />} />
      <Route path="/datenschutz" element={<PrivacyView />} />
      <Route path="*" element={<NotFoundView />} />
    </Routes>
  );
}
