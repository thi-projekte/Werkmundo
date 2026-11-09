import { ArrowLeft, Banknote, ReceiptText } from "lucide-react";
import type { FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { SetupLayout } from "../shared/WerkmundoShell";
import { useInvoiceSetupViewModel } from "../../ViewModel/onboarding/useInvoiceSetupViewModel";

export function InvoiceSetupView() {
  const navigate = useNavigate();
  const { error, finishSetup, form, skipSetup, updateField } = useInvoiceSetupViewModel();

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    finishSetup();
  };

  return (
    <SetupLayout activeStep={2}>
      <form className="wm-setup-card" onSubmit={submit}>
        <div className="wm-setup-intro">
          <div>
            <div className="wm-eyebrow"><span />Ersteinrichtung / Deine Rechnungen</div>
            <h1 className="wm-setup-title">Bereit für <em>Rechnungen.</em></h1>
          </div>
          <p>Angebote sind auch ohne diese Angaben nutzbar, wenn du aber Rechnungen<br />erstellen willst, brauchen wir diese Informationen.</p>
        </div>

        <section className="wm-form-section">
          <h2><ReceiptText aria-hidden="true" />Steuern und Rechnungsangaben</h2>
          <div className="wm-form-grid">
            <label>Umsatzsteuerliche Behandlung*<select required value={form.vatTreatment} onChange={(event) => updateField("vatTreatment", event.target.value)}><option value="">Bitte auswählen</option><option>Umsatzsteuerpflichtig</option><option>Kleinunternehmer (§ 19 UStG)</option><option>Steuerbefreit</option></select></label>
            <label>Steuerliche Kennung*<select required value={form.taxId} onChange={(event) => updateField("taxId", event.target.value)}><option value="">Bitte auswählen</option><option>Steuernummer</option><option>Umsatzsteuer-ID</option></select></label>
            <label>Steuernummer oder USt-IdNr.*<input required value={form.taxNumber} onChange={(event) => updateField("taxNumber", event.target.value)} placeholder="Entsprechend deiner Auswahl" /></label>
            <label>Rechnungsnummer-Präfix*<input required value={form.invoicePrefix} onChange={(event) => updateField("invoicePrefix", event.target.value)} placeholder="RE-2026-" /></label>
            <label>Zahlungsziel*<select required value={form.paymentTerm} onChange={(event) => updateField("paymentTerm", event.target.value)}><option>14 Tage</option><option>7 Tage</option><option>30 Tage</option><option>Sofort fällig</option></select></label>
            <label>Rechnungs-E-Mail (optional)<input type="email" value={form.invoiceEmail} onChange={(event) => updateField("invoiceEmail", event.target.value)} placeholder="buchhaltung@deinefirma.de" /></label>
          </div>
          <p className="wm-form-note">Unser Tool wird dir fortlaufende, eindeutige Rechnungsnummern später automatisch vergeben. Du legst hier nur das Präfix fest.</p>
          <p className="wm-form-tip">› Registerangaben ergänzen. Nur falls zutreffend.</p>
        </section>

        <section className="wm-form-section">
          <div className="wm-section-heading">
            <h2><Banknote aria-hidden="true" />Bankverbindung</h2>
            <span className="wm-recommendation">Für Überweisungen empfohlen</span>
          </div>
          <div className="wm-form-grid">
            <label>Kontoinhaber<input value={form.accountHolder} onChange={(event) => updateField("accountHolder", event.target.value)} placeholder="Name des Kontoinhabers" /></label>
            <label>IBAN<input value={form.iban} onChange={(event) => updateField("iban", event.target.value)} placeholder="DE00 0000 0000 0000 0000 00" /></label>
          </div>
          <p className="wm-form-note">Bankdaten sind für Überweisungen praktisch, aber nicht pauschal gesetzlich vorgeschrieben. Du kannst sie auch nachtragen, wenn du möchtest.</p>
        </section>

        {error && <p className="wm-error" role="alert">{error}</p>}
        <div className="wm-setup-actions wm-invoice-actions">
          <button className="wm-text-button" type="button" onClick={() => navigate("/einrichtung/unternehmen")}>
            <ArrowLeft size={15} />Zur Unternehmenseinrichtung
          </button>
          <div className="wm-button-row">
            <button className="wm-button wm-button-secondary" type="button" onClick={skipSetup}>Überspringen</button>
            <button className="wm-button wm-button-primary" type="submit">Einrichtung abschließen</button>
          </div>
        </div>
      </form>
    </SetupLayout>
  );
}
