import { useEffect, type ReactNode } from "react";
import { ArrowLeft } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import ribbon from "../../shared/assets/werkmundo-ribbon.svg";
import microphone from "../../shared/assets/werkmundo-microphone.svg";
import instagram from "../../shared/assets/werkmundo-instagram.svg";
import linkedin from "../../shared/assets/werkmundo-linkedin.svg";
import tiktok from "../../shared/assets/werkmundo-tiktok.svg";

export function BrandBar() {
  return <div className="wm-brand-bar">HANDWERK. EINFACHER GEMACHT.</div>;
}

function usePageTitle(title: string) {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = `${title} | Werkmundo`;
    return () => {
      document.title = previousTitle;
    };
  }, [title]);
}

export function PageHeading({ title, backTo }: { title: string; backTo?: string }) {
  const navigate = useNavigate();
  const goBack = () => {
    if (backTo) {
      navigate(backTo);
    } else if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate("/");
    }
  };

  return (
    <div className="wm-page-heading">
      <button className="wm-back-button" type="button" onClick={goBack} aria-label="Zurück">
        <ArrowLeft size={17} aria-hidden="true" />
        <span>{title}</span>
      </button>
    </div>
  );
}

export function WerkmundoFooter() {
  return (
    <footer className="wm-footer">
      <nav className="wm-footer-links" aria-label="Rechtliche Informationen">
        <Link to="/kontakt">Kontakt</Link>
        <Link to="/impressum">Impressum</Link>
        <Link to="/datenschutz">Datenschutz</Link>
      </nav>
      <p className="wm-copyright">© 2026 Werkmundo • KI-gestützte Angebote</p>
      <div className="wm-socials" aria-label="Social Media">
        <img src={instagram} alt="" />
        <img src={linkedin} alt="" />
        <img src={tiktok} alt="" />
      </div>
    </footer>
  );
}

export function AuthLayout({
  title,
  backTo,
  children,
}: {
  title: string;
  backTo?: string;
  children: ReactNode;
}) {
  usePageTitle(title);

  return (
    <div className="werkmundo">
      <BrandBar />
      <PageHeading title={title} backTo={backTo ?? (title === "Login" ? "/" : "/login")} />
      <main className="wm-auth-layout">
        <div className="wm-auth-copy">
          {children}
        </div>
        <aside className="wm-brand-card" aria-label="Werkmundo">
          <div className="wm-vertical-wordmark">
            <span>Werk</span><span>mundo</span>
          </div>
          <div className="wm-card-illustration" aria-hidden="true">
            <img className="wm-ribbon" src={ribbon} alt="" />
            <img className="wm-microphone" src={microphone} alt="" />
          </div>
        </aside>
      </main>
      <WerkmundoFooter />
    </div>
  );
}

export function LegalLayout({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  usePageTitle(title);

  return (
    <div className="werkmundo wm-legal-layout">
      <BrandBar />
      <PageHeading title={title} />
      <main className="wm-legal-content">{children}</main>
      <WerkmundoFooter />
    </div>
  );
}

export function SetupLayout({
  activeStep,
  children,
}: {
  activeStep: 1 | 2;
  children: ReactNode;
}) {
  const title = "Registrieren";
  usePageTitle(activeStep === 1 ? "Unternehmenseinrichtung" : "Rechnungseinrichtung");

  return (
    <div className="werkmundo wm-setup-page">
      <BrandBar />
      <PageHeading
        title={title}
        backTo={activeStep === 1 ? "/registrieren" : "/einrichtung/unternehmen"}
      />
      <main className="wm-setup-layout">
        <aside className="wm-setup-sidebar">
          <p className="wm-sidebar-kicker">EINMAL EINRICHTEN. LOSLEGEN.</p>
          <h2>
            Gut vorbereitet.
            <span>Direkt loslegen.</span>
          </h2>
          <p className="wm-sidebar-description">
            Ein paar Firmendaten genügen für den Start. Die Rechnungsdaten richten wir danach ein.
          </p>
          <ol className="wm-step-list">
            <li className={activeStep === 1 ? "is-active" : "is-complete"}>
              <span className="wm-step-number">01</span>
              <span><strong>Für den Start</strong><small>Unternehmen und Mitarbeiter</small></span>
            </li>
            <li className={activeStep === 2 ? "is-active" : ""}>
              <span className="wm-step-number">02</span>
              <span><strong>Für die Rechnungen</strong><small>Steuern und Zahlungseingänge</small></span>
            </li>
          </ol>
        </aside>
        <div className="wm-setup-content">{children}</div>
      </main>
    </div>
  );
}
