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
import { AppLayout } from "./shared/components/AppLayout";
import { HomeView as VoiceHomeView } from "./View/voice/components/HomeView";
import { CustomerListView } from "./View/customer/CustomerListView";
import { CustomerDetailView } from "./View/customer/CustomerDetailView";
import { DocumentListView } from "./View/document/DocumentListView";
import { ProfileView } from "./View/user/ProfileView";
import { OfferReviewView } from "./View/offer/OfferReviewView";
import { OfferShareView } from "./View/offer/OfferShareView";
import { LogoutView } from "./View/user/LogoutView";

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
      <Route path="/einrichtung/unternehmen" element={<CompanySetupView />} />
      <Route path="/einrichtung/rechnungen" element={<InvoiceSetupView />} />
      <Route path="/kontakt" element={<ContactView />} />
      <Route path="/impressum" element={<ImprintView />} />
      <Route path="/datenschutz" element={<PrivacyView />} />
      <Route path="/angebot/versendet" element={<OfferShareView />} />
      <Route path="/abmelden" element={<LogoutView />} />
      <Route path="/logout" element={<LogoutView />} />
      <Route element={<AppLayout />}>
        <Route path="/dashboard" element={<DashboardView />} />
        <Route path="/home" element={<VoiceHomeView />} />
        <Route path="/dokumente" element={<DocumentListView />} />
        <Route path="/kunden" element={<CustomerListView />} />
        <Route path="/kunden/neu" element={<CustomerListView />} />
        <Route path="/kunden/:customerId/bearbeiten" element={<CustomerListView />} />
        <Route path="/kunden/:customerId" element={<CustomerDetailView />} />
        <Route path="/unternehmen" element={<Navigate to="/kunden" replace />} />
        <Route path="/profil" element={<ProfileView />} />
        <Route path="/angebote/:businessKey/bearbeiten" element={<OfferReviewView />} />
      </Route>
      <Route path="*" element={<NotFoundView />} />
    </Routes>
  );
}
