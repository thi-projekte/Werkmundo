import { Link, Route, Routes } from "react-router-dom";
import { AuthLayout } from "./View/shared/WerkmundoShell";
import { LoginView } from "./View/user/LoginView";
import { RegistrationView } from "./View/user/RegistrationView";
import { ContactView } from "./View/legal/ContactView";
import { ImprintView } from "./View/legal/ImprintView";
import { PrivacyView } from "./View/legal/PrivacyView";
import { DashboardView } from "./View/dashboard/DashboardView";
import { CompanySetupView } from "./View/onboarding/CompanySetupView";
import { InvoiceSetupView } from "./View/onboarding/InvoiceSetupView";
import { AppLayout } from "./View/shared/navigation/AppLayout";
import { InvoiceView } from "./View/document/InvoiceView";
import { CustomerListView } from "./View/customer/CustomerListView";
import { ProfileView } from "./View/user/ProfileView";
import { RequireAuth } from "./View/user/RequireAuth";

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
      <Route
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route path="/dashboard" element={<DashboardView />} />
        <Route path="/rechnungen" element={<InvoiceView />} />
        <Route path="/kunden" element={<CustomerListView />} />
        <Route path="/profil" element={<ProfileView />} />
      </Route>
      <Route
        path="/einrichtung/unternehmen"
        element={
          <RequireAuth>
            <CompanySetupView />
          </RequireAuth>
        }
      />
      <Route
        path="/einrichtung/rechnungen"
        element={
          <RequireAuth>
            <InvoiceSetupView />
          </RequireAuth>
        }
      />
      <Route path="/kontakt" element={<ContactView />} />
      <Route path="/impressum" element={<ImprintView />} />
      <Route path="/datenschutz" element={<PrivacyView />} />
      <Route path="*" element={<NotFoundView />} />
    </Routes>
  );
}
