import { Plus, Trash2, UserRound } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { SetupLayout } from "../shared/WerkmundoShell";
import { useCompanySetupViewModel } from "../../ViewModel/onboarding/useCompanySetupViewModel";

export function EmployeeSetupView() {
  const navigate = useNavigate();
  const {
    addEmployee,
    continueToInvoices,
    skipEmployeeSetup,
    error,
    form,
    removeEmployee,
    updateEmployee,
  } = useCompanySetupViewModel();

  return (
    <SetupLayout activeStep={2}>
      <form
        className="wm-setup-card"
        onSubmit={(event) => {
          event.preventDefault();
          continueToInvoices();
        }}
      >
        <div className="wm-setup-intro">
          <div>
            <div className="wm-eyebrow"><span />Ersteinrichtung / Deine Mitarbeiter</div>
            <h1 className="wm-setup-title">Dein Team <em>einrichten.</em></h1>
          </div>
          <p>Lege Mitarbeitergruppen und Verrechnungssätze fest.<br />Du kannst diesen Schritt auch überspringen.</p>
        </div>

        <section className="wm-form-section wm-employees-section">
          <div className="wm-section-heading">
            <h2><UserRound aria-hidden="true" />Deine Mitarbeiter</h2>
            <button className="wm-add-button" type="button" onClick={addEmployee} aria-label="Mitarbeiter hinzufügen"><Plus size={19} /></button>
          </div>
          <div className="wm-employee-grid">
            {form.employees.map((employee, index) => (
              <div className="wm-employee-row" key={`employee-${index}`}>
                <label>Mitarbeitergruppe*<select required value={employee.group} onChange={(event) => updateEmployee(index, "group", event.target.value)}><option value="">Bitte auswählen</option><option>Geselle</option><option>Meister</option><option>Auszubildende</option><option>Helfer</option></select></label>
                <label>Verrechnungssatz netto*<span className="wm-rate-input"><input inputMode="decimal" required value={employee.hourlyRate} onChange={(event) => updateEmployee(index, "hourlyRate", event.target.value)} /><span>€/Std.</span></span></label>
                <button className="wm-employee-remove" type="button" onClick={() => removeEmployee(index)} aria-label={`Mitarbeitergruppe ${index + 1} entfernen`}><Trash2 size={16} /></button>
              </div>
            ))}
          </div>
        </section>
        {error && <p className="wm-error" role="alert">{error}</p>}
        <div className="wm-setup-actions">
          <button className="wm-button wm-button-secondary" type="button" onClick={() => navigate("/einrichtung/unternehmen")}>Zurück</button>
          <button className="wm-button wm-button-secondary" type="button" onClick={skipEmployeeSetup}>Überspringen</button>
          <button className="wm-button wm-button-primary" type="submit">Nächster Schritt</button>
        </div>
      </form>
    </SetupLayout>
  );
}
