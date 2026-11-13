import "../dashboard/components/dashboard.css";
import { useState, type Dispatch, type FormEvent, type SetStateAction } from "react";
import { useSearchParams } from "react-router-dom";
import { BriefcaseBusiness, Check, Plus, ReceiptText, Trash2, UsersRound } from "lucide-react";
import {
  COMPANY_SETUP_STORAGE_KEY,
  INVOICE_SETUP_STORAGE_KEY,
  initialCompanySetup,
  initialInvoiceSetup,
  type Employee,
  type CompanySetupForm,
  type InvoiceSetupForm,
} from "../../Model/onboarding/setupForms";

type ProfileTab = "unternehmen" | "rechnungen" | "abo";

function readStoredForm<T>(key: string, initialValue: T): { value: T; error: string } {
  try {
    const stored = sessionStorage.getItem(key);
    return {
      value: stored ? { ...initialValue, ...JSON.parse(stored) } : initialValue,
      error: "",
    };
  } catch (error) {
    console.error("Profilangaben konnten nicht geladen werden:", error);
    return {
      value: initialValue,
      error: "Gespeicherte Profilangaben konnten nicht geladen werden.",
    };
  }
}

const plans = [
  { id: "kompakt", label: "KOMPAKT", offers: 20, price: "39,99", description: "Für den Einstieg und gelegentliche Angebote." },
  { id: "alltag", label: "ALLTAG", offers: 40, price: "54,99", description: "Für Betriebe mit regelmäßigen Angebotsbedarf." },
  { id: "vielnutzer", label: "VIELNUTZER", offers: 60, price: "67,99", description: "Für Betriebe mit hohem Angebotsvolumen." },
] as const;

type PlanId = (typeof plans)[number]["id"];

const PLAN_STORAGE_KEY = "werkmundo-selected-plan";

function readStoredPlan(): { value: PlanId; error: string } {
  try {
    const stored = sessionStorage.getItem(PLAN_STORAGE_KEY);
    return {
      value: plans.find((plan) => plan.id === stored)?.id ?? "alltag",
      error: "",
    };
  } catch (error) {
    console.error("Tarifauswahl konnte nicht geladen werden:", error);
    return { value: "alltag", error: "Gespeicherte Tarifauswahl konnte nicht geladen werden." };
  }
}

export function ProfileView() {
  const [initialCompany] = useState(() => readStoredForm(COMPANY_SETUP_STORAGE_KEY, initialCompanySetup));
  const [initialInvoice] = useState(() => readStoredForm(INVOICE_SETUP_STORAGE_KEY, initialInvoiceSetup));
  const [initialPlan] = useState(readStoredPlan);
  const [searchParams, setSearchParams] = useSearchParams();
  const tabValue = searchParams.get("tab");
  const tab: ProfileTab = tabValue === "rechnungen" || tabValue === "abo" ? tabValue : "unternehmen";
  const [company, setCompany] = useState<CompanySetupForm>(initialCompany.value);
  const [invoice, setInvoice] = useState<InvoiceSetupForm>(initialInvoice.value);
  const [editingCompany, setEditingCompany] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<PlanId>(initialPlan.value);
  const [fileName, setFileName] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState(initialCompany.error || initialInvoice.error || initialPlan.error);

  const saveCompany = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    try {
      sessionStorage.setItem(COMPANY_SETUP_STORAGE_KEY, JSON.stringify(company));
      setEditingCompany(false);
      setNotice("Unternehmensangaben für diese Sitzung gespeichert.");
    } catch (saveError) {
      console.error("Unternehmensangaben konnten nicht gespeichert werden:", saveError);
      setError("Unternehmensangaben konnten nicht gespeichert werden.");
    }
  };

  const saveInvoice = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    try {
      sessionStorage.setItem(INVOICE_SETUP_STORAGE_KEY, JSON.stringify(invoice));
      setNotice("Rechnungsangaben für diese Sitzung gespeichert.");
    } catch (saveError) {
      console.error("Rechnungsangaben konnten nicht gespeichert werden:", saveError);
      setError("Rechnungsangaben konnten nicht gespeichert werden.");
    }
  };

  const savePlan = () => {
    setError("");
    try {
      sessionStorage.setItem(PLAN_STORAGE_KEY, selectedPlan);
      setNotice("Deine Tarifauswahl wurde für diese Sitzung gespeichert.");
    } catch (saveError) {
      console.error("Tarifauswahl konnte nicht gespeichert werden:", saveError);
      setError("Tarifauswahl konnte nicht gespeichert werden.");
    }
  };

  const cancelCompanyEdit = () => {
    setCompany(readStoredForm(COMPANY_SETUP_STORAGE_KEY, initialCompanySetup).value);
    setEditingCompany(false);
    setError("");
    setNotice("");
  };

  const cancelInvoiceEdit = () => {
    setInvoice(readStoredForm(INVOICE_SETUP_STORAGE_KEY, initialInvoiceSetup).value);
    setError("");
    setNotice("");
  };

  const cancelPlanEdit = () => {
    const storedPlan = readStoredPlan();
    setSelectedPlan(storedPlan.value);
    setError(storedPlan.error);
    setNotice("");
  };

  const addEmployee = () => {
    setCompany((current) => ({
      ...current,
      employees: [...current.employees, { group: "", hourlyRate: "" }],
    }));
    setEditingCompany(true);
  };

  const updateCompany = (field: keyof CompanySetupForm, value: string) => {
    setCompany((current) => ({ ...current, [field]: value }));
  };
  const updateInvoice = (field: keyof InvoiceSetupForm, value: string) => {
    setInvoice((current) => ({ ...current, [field]: value }));
  };

  return (
    <div className="dashboard-page wm-dashboard wm-profile-page">
      <header className="wm-dashboard-heading">
        <div>
          <h1>Dein <span>Profil</span></h1>
          <p className="wm-dashboard-description">Verwalte deine Unternehmens- und Rechnungseinstellungen.</p>
        </div>
      </header>

      <section className="wm-list-panel wm-profile-panel">
        <div className="wm-profile-tabs" role="tablist" aria-label="Profileinstellungen">
          <button type="button" role="tab" aria-selected={tab === "unternehmen"} className={tab === "unternehmen" ? "is-active" : ""} onClick={() => setSearchParams({ tab: "unternehmen" })}>Unternehmen</button>
          <button type="button" role="tab" aria-selected={tab === "rechnungen"} className={tab === "rechnungen" ? "is-active" : ""} onClick={() => setSearchParams({ tab: "rechnungen" })}>Rechnungen</button>
          <button type="button" role="tab" aria-selected={tab === "abo"} className={tab === "abo" ? "is-active" : ""} onClick={() => setSearchParams({ tab: "abo" })}>Abo-Modell</button>
        </div>

        {notice && <p className="wm-dashboard-feedback wm-dashboard-success" role="status">{notice}</p>}
        {error && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{error}</p>}

        {tab === "unternehmen" && (
          <div className="wm-profile-content">
            <section className="wm-profile-company-card">
              <span className="wm-profile-company-mark"><BriefcaseBusiness size={22} aria-hidden="true" /></span>
              <span>
                <strong>{company.companyName || "Dein Unternehmen"}</strong>
                <small>{company.trade || "Unternehmensprofil"}</small>
              </span>
            </section>

            {editingCompany ? (
              <form className="wm-profile-form" onSubmit={saveCompany}>
                <section className="wm-profile-section">
                  <div className="wm-section-heading"><h2><BriefcaseBusiness size={17} aria-hidden="true" />Dein Unternehmen</h2></div>
                  <div className="wm-profile-data-grid">
                    <label>Firmenname<input value={company.companyName} onChange={(event) => updateCompany("companyName", event.target.value)} /></label>
                    <label>Rechtsform<input value={company.legalForm} onChange={(event) => updateCompany("legalForm", event.target.value)} /></label>
                    <label>Branche/Gewerk<input value={company.trade} onChange={(event) => updateCompany("trade", event.target.value)} /></label>
                    <label>Geschäftliche E-Mail<input type="email" value={company.email} onChange={(event) => updateCompany("email", event.target.value)} /></label>
                    <label>Ansprechpartner<input value={company.contact} onChange={(event) => updateCompany("contact", event.target.value)} /></label>
                    <label>Telefonnummer<input type="tel" value={company.phone} onChange={(event) => updateCompany("phone", event.target.value)} /></label>
                    <label>Straße und Hausnummer<input value={company.street} onChange={(event) => updateCompany("street", event.target.value)} /></label>
                    <label>Postleitzahl<input value={company.postalCode} onChange={(event) => updateCompany("postalCode", event.target.value)} /></label>
                    <label>Ort<input value={company.city} onChange={(event) => updateCompany("city", event.target.value)} /></label>
                    <label>Land<input value={company.country} onChange={(event) => updateCompany("country", event.target.value)} /></label>
                  </div>
                </section>
                <EmployeeEditor company={company} setCompany={setCompany} />
                <div className="wm-profile-actions">
                  <button className="wm-button wm-button-secondary" type="button" onClick={cancelCompanyEdit}>Abbrechen</button>
                  <button className="wm-button wm-button-primary" type="submit">Speichern</button>
                </div>
              </form>
            ) : (
              <>
                <section className="wm-profile-section">
                  <div className="wm-section-heading">
                    <h2><BriefcaseBusiness size={17} aria-hidden="true" />Dein Unternehmen</h2>
                    <div className="wm-profile-heading-actions">
                      <button className="wm-inline-action" type="button" onClick={() => setEditingCompany(true)}>Bearbeiten</button>
                    </div>
                  </div>
                  <dl className="wm-profile-data-grid">
                    <ProfileDatum label="Firmenname" value={company.companyName} />
                    <ProfileDatum label="Rechtsform" value={company.legalForm} />
                    <ProfileDatum label="Branche/Gewerk" value={company.trade} />
                    <ProfileDatum label="Geschäftliche E-Mail" value={company.email} />
                    <ProfileDatum label="Ansprechpartner" value={company.contact} />
                    <ProfileDatum label="Geschäftliche Telefonnummer" value={company.phone} />
                    <ProfileDatum label="Straße und Hausnummer" value={company.street} />
                    <ProfileDatum label="Postleitzahl" value={company.postalCode} />
                    <ProfileDatum label="Ort" value={company.city} />
                    <ProfileDatum label="Land" value={company.country} />
                  </dl>
                </section>
                <section className="wm-profile-section">
                  <div className="wm-section-heading">
                    <h2><UsersRound size={17} aria-hidden="true" />Deine Mitarbeiter</h2>
                    <button className="wm-add-button" type="button" onClick={addEmployee} aria-label="Mitarbeiter hinzufügen"><Plus size={19} /></button>
                  </div>
                  {company.employees.length ? (
                    <ul className="wm-employee-list">
                      {company.employees.map((employee, index) => (
                        <li key={`${employee.group}-${index}`}>
                          <span>{employee.group}</span>
                          <strong>{employee.hourlyRate || "—"} €/Std.</strong>
                          <Check size={15} aria-hidden="true" />
                        </li>
                      ))}
                    </ul>
                  ) : <p className="wm-empty-state">Noch keine Mitarbeiter hinzugefügt.</p>}
                </section>
              </>
            )}
          </div>
        )}

        {tab === "rechnungen" && (
          <form className="wm-profile-form" onSubmit={saveInvoice}>
            <section className="wm-profile-section">
              <div className="wm-section-heading"><h2><ReceiptText size={17} aria-hidden="true" />Steuern und Rechnungsangaben</h2></div>
              <div className="wm-profile-data-grid">
                <label>Umsatzsteuerliche Behandlung<select value={invoice.vatTreatment} onChange={(event) => updateInvoice("vatTreatment", event.target.value)}><option value="">Bitte auswählen</option><option>Regelbesteuerung</option><option>Kleinunternehmerregelung</option></select></label>
                <label>Steuerliche Kennung<input value={invoice.taxId} onChange={(event) => updateInvoice("taxId", event.target.value)} placeholder="Bitte auswählen" /></label>
                <label>Steuernummer<input value={invoice.taxNumber} onChange={(event) => updateInvoice("taxNumber", event.target.value)} /></label>
                <label>Rechnungsnummer-Präfix<input value={invoice.invoicePrefix} onChange={(event) => updateInvoice("invoicePrefix", event.target.value)} /></label>
                <label>Zahlungsziel<select value={invoice.paymentTerm} onChange={(event) => updateInvoice("paymentTerm", event.target.value)}><option>14 Tage</option><option>7 Tage</option><option>30 Tage</option></select></label>
                <label>Rechnungs-E-Mail<input type="email" value={invoice.invoiceEmail} onChange={(event) => updateInvoice("invoiceEmail", event.target.value)} /></label>
              </div>
              <p className="wm-profile-hint">Deine Angaben werden für die Erstellung von Rechnungsdokumenten verwendet.</p>
            </section>
            <section className="wm-profile-section">
              <div className="wm-section-heading"><h2><ReceiptText size={17} aria-hidden="true" />Bankverbindung</h2></div>
              <div className="wm-profile-data-grid">
                <label>Kontoinhaber<input value={invoice.accountHolder} onChange={(event) => updateInvoice("accountHolder", event.target.value)} /></label>
                <label>IBAN<input value={invoice.iban} onChange={(event) => updateInvoice("iban", event.target.value)} autoComplete="off" /></label>
              </div>
            </section>
            <section className="wm-profile-section">
              <div className="wm-section-heading"><h2><ReceiptText size={17} aria-hidden="true" />Materialpreisliste (CSV)</h2></div>
              <label className="wm-upload-field">
                <input type="file" accept=".csv,text/csv" onChange={(event) => setFileName(event.target.files?.[0]?.name ?? "")} />
                <span>{fileName || "Hier klicken, um eine Materialpreisliste auszuwählen (CSV)."}</span>
              </label>
            </section>
            <div className="wm-profile-actions">
              <button className="wm-button wm-button-secondary" type="button" onClick={cancelInvoiceEdit}>Abbrechen</button>
              <button className="wm-button wm-button-primary" type="submit">Speichern</button>
            </div>
          </form>
        )}

        {tab === "abo" && (
          <div className="wm-profile-content wm-subscription-content">
            <p className="wm-profile-intro">Wähle dein passendes Modell aus.</p>
            <div className="wm-plan-grid">
              {plans.map((plan) => (
                <article className={`wm-plan-card ${selectedPlan === plan.id ? "is-selected" : ""}`} key={plan.id}>
                  <p>{plan.label}</p>
                  <h3>{plan.offers}<span> Angebote / Monat</span></h3>
                  <small>{plan.description}</small>
                  <div className="wm-plan-price"><strong>{plan.price} €</strong><span>pro Monat</span></div>
                  <button type="button" className={selectedPlan === plan.id ? "is-selected" : ""} onClick={() => setSelectedPlan(plan.id)}>
                    {selectedPlan === plan.id ? <><Check size={14} aria-hidden="true" /> Dein Tarif</> : "Tarif wählen"}
                  </button>
                </article>
              ))}
            </div>
            <p className="wm-profile-hint">Unser Enterprise-Modell gibt es nur auf Anfrage. <a href="mailto:kontakt@werkmundo.de">Klicke hier.</a></p>
            <div className="wm-profile-actions">
              <button className="wm-button wm-button-secondary" type="button" onClick={cancelPlanEdit}>Abbrechen</button>
              <button className="wm-button wm-button-primary" type="button" onClick={savePlan}>Speichern</button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

function ProfileDatum({ label, value }: { label: string; value: string }) {
  return <div><dt>{label}</dt><dd>{value || "Noch nicht hinterlegt"}</dd></div>;
}

function EmployeeEditor({
  company,
  setCompany,
}: {
  company: CompanySetupForm;
  setCompany: Dispatch<SetStateAction<CompanySetupForm>>;
}) {
  const updateEmployee = (index: number, update: Partial<Employee>) => {
    setCompany((current) => ({
      ...current,
      employees: current.employees.map((employee, employeeIndex) =>
        employeeIndex === index ? { ...employee, ...update } : employee,
      ),
    }));
  };

  const removeEmployee = (index: number) => {
    setCompany((current) => ({
      ...current,
      employees: current.employees.filter((_, employeeIndex) => employeeIndex !== index),
    }));
  };

  const addEmployee = () => {
    setCompany((current) => ({
      ...current,
      employees: [...current.employees, { group: "", hourlyRate: "" }],
    }));
  };

  return (
    <section className="wm-profile-section">
      <div className="wm-section-heading">
        <h2><UsersRound size={17} aria-hidden="true" />Deine Mitarbeiter</h2>
        <button className="wm-add-button" type="button" onClick={addEmployee} aria-label="Mitarbeiter hinzufügen"><Plus size={19} /></button>
      </div>
      <div className="wm-profile-data-grid">
        {company.employees.map((employee, index) => (
          <div className="wm-employee-edit-row" key={`employee-${index}`}>
            <label>
              Mitarbeitergruppe
              <select required value={employee.group} onChange={(event) => updateEmployee(index, { group: event.target.value })}>
                <option value="">Gruppe auswählen</option>
                <option value="Geselle">Geselle</option>
                <option value="Meister">Meister</option>
                <option value="Helfer">Helfer</option>
                <option value="Auszubildende">Auszubildende</option>
              </select>
            </label>
            <label>
              Verrechnungssatz netto
              <input
                required
                inputMode="decimal"
                placeholder="z. B. 75,00"
                value={employee.hourlyRate}
                onChange={(event) => updateEmployee(index, { hourlyRate: event.target.value })}
              />
            </label>
            <button
              className="wm-employee-remove"
              type="button"
              onClick={() => removeEmployee(index)}
              aria-label={`Mitarbeitergruppe ${employee.group || index + 1} entfernen`}
            >
              <Trash2 size={16} aria-hidden="true" />
            </button>
          </div>
        ))}
      </div>
    </section>
  );
}