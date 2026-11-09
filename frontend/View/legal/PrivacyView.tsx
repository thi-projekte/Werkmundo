import { privacySections } from "../../Model/legal/legalContent";
import { LegalLayout } from "../shared/WerkmundoShell";

export function PrivacyView() {
  return (
    <LegalLayout title="Datenschutz">
      <div className="wm-eyebrow"><span />INFORMATION ZUR VERARBEITUNG VON PERSONENBEZOGENEN DATEN</div>
      <h1 className="wm-legal-title">Datenschutzerklärung.</h1>
      <div className="wm-legal-sections wm-privacy-sections">
        {privacySections.map((section) => (
          <section className="wm-legal-section" key={section.heading}>
            <h2>{section.heading}</h2>
            <p>{section.body}</p>
          </section>
        ))}
      </div>
    </LegalLayout>
  );
}
