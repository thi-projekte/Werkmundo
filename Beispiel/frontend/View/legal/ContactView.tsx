import { LegalLayout } from "../shared/WerkmundoShell";

export function ContactView() {
  return (
    <LegalLayout title="Kontakt">
      <div className="wm-eyebrow"><span />WIR HELFEN DIR GERNE WEITER</div>
      <h1 className="wm-legal-title">Kontakt.</h1>
      <div className="wm-legal-sections">
        <section className="wm-legal-section">
          <h2>Kontakt</h2>
          <p><a href="mailto:kontakt@werkmundo.de">kontakt@werkmundo.de</a></p>
        </section>
        <section className="wm-legal-section">
          <h2>Projektinformationen</h2>
          <p>Das Projekt ist ein Studienprojekt der Technischen Hochschule Ingolstadt.</p>
        </section>
      </div>
    </LegalLayout>
  );
}
