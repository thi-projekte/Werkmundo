import { BriefcaseBusiness } from "lucide-react";
import type { FormEvent } from "react";
import { SetupLayout } from "../shared/WerkmundoShell";
import { useCompanySetupViewModel } from "../../ViewModel/onboarding/useCompanySetupViewModel";

export function CompanySetupView() {
  const { continueToEmployees, skipSetup, error, form, updateField } =
    useCompanySetupViewModel();

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    continueToEmployees();
  };

  return (
    <SetupLayout activeStep={1}>
      <form className="wm-setup-card" onSubmit={submit}>
        <div className="wm-setup-intro">
          <div>
            <div className="wm-eyebrow"><span />Ersteinrichtung / Dein Unternehmen</div>
            <h1 className="wm-setup-title">Mach Titel <em>startklar.</em></h1>
          </div>
          <p>Gib hier die Informationen über dein Unternehmen an,<br />die wir brauchen, um damit Angebote zu erstellen.</p>
        </div>

        <section className="wm-form-section">
          <h2><BriefcaseBusiness aria-hidden="true" />Dein Unternehmen</h2>
          <div className="wm-form-grid wm-form-grid-three">
            <label>Firmenname*<input required value={form.companyName} onChange={(event) => updateField("companyName", event.target.value)} placeholder="z. B. Max Mustermann GmbH" /></label>
            <label>Rechtsform*<select required value={form.legalForm} onChange={(event) => updateField("legalForm", event.target.value)}><option value="">Bitte auswählen</option><option>Einzelunternehmen</option><option>GbR</option><option>GmbH</option><option>UG</option><option>Sonstige</option></select></label>
            <label>Branche/Gewerk*<select required value={form.trade} onChange={(event) => updateField("trade", event.target.value)}><option value="">Bitte auswählen</option><option>Elektro</option><option>Sanitär & Heizung</option><option>Schreinerei</option><option>Maler</option><option>Sonstiges</option></select></label>
            <label>Geschäftliche E-Mail*<input type="email" required value={form.email} onChange={(event) => updateField("email", event.target.value)} placeholder="kontakt@deinefirma.de" /></label>
            <label>Ansprechpartner*<input required value={form.contact} onChange={(event) => updateField("contact", event.target.value)} placeholder="Vorname Nachname" /></label>
            <label>Geschäftliche Telefonnummer (optional)<input type="tel" value={form.phone} onChange={(event) => updateField("phone", event.target.value)} placeholder="+49 89 123456" /></label>
            <label className="wm-field-half">Straße und Hausnummer*<input required value={form.street} onChange={(event) => updateField("street", event.target.value)} placeholder="Musterstraße 12" /></label>
            <label className="wm-field-half">Postleitzahl*<input required value={form.postalCode} onChange={(event) => updateField("postalCode", event.target.value)} placeholder="80331" /></label>
            <label className="wm-field-half">Ort*<input required value={form.city} onChange={(event) => updateField("city", event.target.value)} placeholder="München" /></label>
            <label className="wm-field-half">Land*<input required value={form.country} onChange={(event) => updateField("country", event.target.value)} /></label>
          </div>
        </section>
        {error && <p className="wm-error" role="alert">{error}</p>}
        <div className="wm-setup-actions">
          <button className="wm-button wm-button-secondary" type="button" onClick={skipSetup}>Überspringen</button>
          <button className="wm-button wm-button-primary" type="submit">Nächster Schritt</button>
        </div>
      </form>
    </SetupLayout>
  );
}
