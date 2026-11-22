import "../dashboard/components/dashboard.css";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ArrowUpRight, Minus, Plus, Search, UserRound } from "lucide-react";
import {
  createCustomer, getCustomer, getCustomers, updateCustomer,
  type CustomerInput, type CustomerProfile,
} from "../../infrastructure/api/userApi";

const emptyCustomer: CustomerInput = {
  firstName: "", lastName: "", email: "", phoneNumber: "",
  companyName: "", street: "", houseNumber: "", zipCode: "",
  city: "", country: "Deutschland",
};

type CustomerType = "alle" | "privat" | "unternehmen";
type PhoneType = "alle" | "mit" | "ohne";
type AddressType = "alle" | "vollstaendig" | "unvollstaendig";

function customerName(customer: CustomerProfile) {
  return [customer.firstName, customer.lastName].filter(Boolean).join(" ") || customer.email;
}

function norm(value?: string | null) {
  return (value ?? "").trim().toLocaleLowerCase("de");
}

function hasFullAddress(customer: CustomerProfile) {
  return Boolean(norm(customer.street) && norm(customer.houseNumber) && norm(customer.zipCode) && norm(customer.city));
}

export function CustomerListView() {
  const { customerId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const isCreating = location.pathname.endsWith("/neu");
  const isEditing = customerId !== undefined;
  const showForm = isCreating || isEditing;
  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [form, setForm] = useState<CustomerInput>(emptyCustomer);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [formLoading, setFormLoading] = useState(isEditing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [filtersOpen, setFiltersOpen] = useState(false);
  const [customerType, setCustomerType] = useState<CustomerType>("alle");
  const [cityFilter, setCityFilter] = useState("");
  const [phoneFilter, setPhoneFilter] = useState<PhoneType>("alle");
  const [addressFilter, setAddressFilter] = useState<AddressType>("alle");

  useEffect(() => {
    let active = true;
    void getCustomers()
      .then((result) => { if (active) setCustomers(result); })
      .catch((loadError: unknown) => {
        if (active) setError(loadError instanceof Error
          ? `Kunden konnten nicht geladen werden: ${loadError.message}`
          : "Kunden konnten nicht geladen werden.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!customerId) {
      setForm(emptyCustomer);
      setFormLoading(false);
      return;
    }
    const id = Number(customerId);
    if (!Number.isSafeInteger(id) || id < 1) {
      setError("Die Kunden-ID ist ungültig.");
      setFormLoading(false);
      return;
    }
    let active = true;
    setFormLoading(true);
    void getCustomer(id)
      .then((customer) => { if (active) setForm(customer); })
      .catch((loadError: unknown) => {
        if (active) setError(loadError instanceof Error
          ? `Kundendaten konnten nicht geladen werden: ${loadError.message}`
          : "Kundendaten konnten nicht geladen werden.");
      })
      .finally(() => { if (active) setFormLoading(false); });
    return () => { active = false; };
  }, [customerId]);

  const cities = useMemo(() => [...new Set(customers.map((c) => c.city?.trim() ?? "").filter(Boolean))]
    .sort((a, b) => a.localeCompare(b, "de")), [customers]);
  const activeFilters = [customerType !== "alle", Boolean(cityFilter), phoneFilter !== "alle", addressFilter !== "alle"]
    .filter(Boolean).length;

  const filteredCustomers = useMemo(() => {
    const query = norm(search);
    return customers.filter((customer) => {
      if (query && !norm([
        customer.firstName, customer.lastName, customer.email, customer.phoneNumber,
        customer.companyName, customer.street, customer.houseNumber, customer.zipCode, customer.city,
      ].filter(Boolean).join(" ")).includes(query)) return false;
      const business = Boolean(norm(customer.companyName));
      if (customerType === "unternehmen" && !business) return false;
      if (customerType === "privat" && business) return false;
      if (cityFilter && norm(customer.city) !== norm(cityFilter)) return false;
      const hasPhone = Boolean(norm(customer.phoneNumber));
      if (phoneFilter === "mit" && !hasPhone) return false;
      if (phoneFilter === "ohne" && hasPhone) return false;
      const fullAddress = hasFullAddress(customer);
      if (addressFilter === "vollstaendig" && !fullAddress) return false;
      if (addressFilter === "unvollstaendig" && fullAddress) return false;
      return true;
    });
  }, [customers, search, customerType, cityFilter, phoneFilter, addressFilter]);

  const resetFilters = () => {
    setCustomerType("alle"); setCityFilter(""); setPhoneFilter("alle"); setAddressFilter("alle");
  };

  const updateField = (field: keyof CustomerInput, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const saved = isEditing
        ? await updateCustomer(Number(customerId), form)
        : await createCustomer(form);
      navigate(`/kunden/${saved.id}`);
    } catch (saveError: unknown) {
      setError(saveError instanceof Error
        ? `Kunde konnte nicht gespeichert werden: ${saveError.message}`
        : "Kunde konnte nicht gespeichert werden.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="dashboard-page wm-dashboard wm-crm-page">
      <header className="wm-dashboard-heading">
        <div>
          <h1>Deine <span>Kunden</span></h1>
          <p className="wm-dashboard-description">Verwalte deine Kundendaten und behalte alles im Blick.</p>
        </div>
      </header>

      {showForm && (
        <form className="wm-crm-form-panel" onSubmit={submit}>
          <div className="wm-section-heading">
            <h2><UserRound size={18} aria-hidden="true" />{isEditing ? "Kundendaten bearbeiten" : "Deine Kunden"}</h2>
            <Link className="wm-add-button" to="/kunden" aria-label="Formular schließen" title="Formular schließen">
              <Minus size={19} aria-hidden="true" />
            </Link>
          </div>
          {formLoading ? (
            <p className="wm-dashboard-feedback">Kundendaten werden geladen …</p>
          ) : (
            <>
              <div className="wm-crm-form-grid">
                <label>Vorname*<input required value={form.firstName ?? ""} onChange={(event) => updateField("firstName", event.target.value)} autoComplete="given-name" /></label>
                <label>Nachname*<input required value={form.lastName ?? ""} onChange={(event) => updateField("lastName", event.target.value)} autoComplete="family-name" /></label>
                <label>E-Mail-Adresse*<input required type="email" value={form.email} onChange={(event) => updateField("email", event.target.value)} autoComplete="email" /></label>
                <label>Telefonnummer<input type="tel" value={form.phoneNumber ?? ""} onChange={(event) => updateField("phoneNumber", event.target.value)} autoComplete="tel" /></label>
                <label>Unternehmen<input value={form.companyName ?? ""} onChange={(event) => updateField("companyName", event.target.value)} autoComplete="organization" /></label>
                <label>Straße<input value={form.street ?? ""} onChange={(event) => updateField("street", event.target.value)} autoComplete="address-line1" /></label>
                <label>Hausnummer<input value={form.houseNumber ?? ""} onChange={(event) => updateField("houseNumber", event.target.value)} /></label>
                <label>Postleitzahl<input value={form.zipCode ?? ""} onChange={(event) => updateField("zipCode", event.target.value)} autoComplete="postal-code" /></label>
                <label>Ort<input value={form.city ?? ""} onChange={(event) => updateField("city", event.target.value)} autoComplete="address-level2" /></label>
              </div>
              <div className="wm-crm-form-actions">
                <button className="wm-button wm-button-secondary" type="button" onClick={() => navigate("/kunden")}>Abbrechen</button>
                <button className="wm-button wm-button-primary" type="submit" disabled={saving || formLoading}>
                  {saving ? "Wird gespeichert …" : isEditing ? "Kunde speichern" : "Kunde anlegen"}
                </button>
              </div>
            </>
          )}
        </form>
      )}

      <section className="wm-list-panel wm-customer-list-panel" aria-label="Kundenliste">
        <div className="wm-customer-list-toolbar">
          <div className="wm-customer-list-label">
            <span className="wm-list-icon"><UserRound size={17} aria-hidden="true" /></span>
            <span><strong>Deine Kunden</strong><small>Lege hier neue Kunden an.</small></span>
          </div>
          {!showForm && (
            <Link className="wm-add-button" to="/kunden/neu" aria-label="Kunden hinzufügen">
              <Plus size={19} aria-hidden="true" />
            </Link>
          )}
        </div>

        <div className="wm-document-toolbar">
          <label className="wm-search-field">
            <Search size={16} aria-hidden="true" />
            <input value={search} onChange={(event) => setSearch(event.target.value)}
              placeholder="Suche nach Name, E-Mail, Telefonnummer, Adresse oder PLZ" />
          </label>
          <button type="button" className="wm-filter-label wm-customer-filter-trigger"
            aria-expanded={filtersOpen} aria-controls="wm-customer-filters"
            onClick={() => setFiltersOpen((current) => !current)}>
            Filter{activeFilters > 0 ? ` (${activeFilters})` : ""}
          </button>
        </div>

        {filtersOpen && (
          <div className="wm-customer-filter-panel" id="wm-customer-filters">
            <div className="wm-customer-filter-grid">
              <label>Kundentyp
                <select value={customerType} onChange={(event) => setCustomerType(event.target.value as CustomerType)}>
                  <option value="alle">Alle Kunden</option>
                  <option value="privat">Privatkunden (ohne Unternehmen)</option>
                  <option value="unternehmen">Geschäftskunden (mit Unternehmen)</option>
                </select>
              </label>
              <label>Ort
                <select value={cityFilter} onChange={(event) => setCityFilter(event.target.value)}>
                  <option value="">Alle Orte</option>
                  {cities.map((city) => <option key={city} value={city}>{city}</option>)}
                </select>
              </label>
              <label>Telefonnummer
                <select value={phoneFilter} onChange={(event) => setPhoneFilter(event.target.value as PhoneType)}>
                  <option value="alle">Alle</option>
                  <option value="mit">Mit Telefonnummer</option>
                  <option value="ohne">Ohne Telefonnummer</option>
                </select>
              </label>
              <label>Adresse
                <select value={addressFilter} onChange={(event) => setAddressFilter(event.target.value as AddressType)}>
                  <option value="alle">Alle</option>
                  <option value="vollstaendig">Vollständig (Straße, Nr., PLZ, Ort)</option>
                  <option value="unvollstaendig">Unvollständig</option>
                </select>
              </label>
            </div>
            <div className="wm-customer-filter-footer">
              <span>{filteredCustomers.length} von {customers.length} Kunden</span>
              <button type="button" className="wm-customer-filter-reset" onClick={resetFilters}
                disabled={activeFilters === 0}>Filter zurücksetzen</button>
            </div>
          </div>
        )}

        {error && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{error}</p>}
        {loading ? (
          <p className="wm-dashboard-feedback">Kunden werden geladen …</p>
        ) : error ? null : filteredCustomers.length ? (
          <ul className="wm-customer-items">
            {filteredCustomers.map((customer) => (
              <li key={customer.id}>
                <Link to={`/kunden/${customer.id}`}>
                  <span>
                    <strong>{customerName(customer)}</strong>
                    <small>{[customer.street, customer.city].filter(Boolean).join(", ") || customer.companyName || "Keine Adresse hinterlegt"}</small>
                    <small>{customer.email}{customer.phoneNumber ? ` · ${customer.phoneNumber}` : ""}</small>
                  </span>
                  <span className="wm-customer-arrow"><ArrowUpRight size={17} aria-hidden="true" /></span>
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className="wm-empty-state">
            {search.trim() || activeFilters > 0
              ? "Keine passenden Kunden gefunden. Passe die Suche oder Filter an."
              : "Noch keine Kunden vorhanden. Lege deinen ersten Kunden an."}
          </p>
        )}
      </section>
    </div>
  );
}
