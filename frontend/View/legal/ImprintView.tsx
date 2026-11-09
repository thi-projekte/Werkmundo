import { imprintSections } from "../../Model/legal/legalContent";
import { LegalLayout } from "../shared/WerkmundoShell";

export function ImprintView() {
  return (
    <LegalLayout title="Impressum">
      <div className="wm-eyebrow"><span />RECHTLICHE INFORMATIONEN</div>
      <h1 className="wm-legal-title">Impressum.</h1>
      <p className="wm-subtitle">Angaben gemäß §5 TMG</p>
      <div className="wm-legal-sections">
        {imprintSections.map((section) => (
          <section className="wm-legal-section" key={section.heading}>
            <h2>{section.heading}</h2>
            <p>
              {section.email ? (
                <a href={`mailto:${section.email}`}>{section.body}</a>
              ) : (
                section.body
              )}
            </p>
          </section>
        ))}
      </div>
    </LegalLayout>
  );
}
