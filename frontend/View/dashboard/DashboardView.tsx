import { DashboardOverview } from "./components/DashboardOverview";
import { useDashboard } from "../../ViewModel/dashboard/legacy/useDashboard";
import { getCustomers, type CustomerProfile } from "../../infrastructure/api/userApi";
import { Mic, Plus } from "lucide-react";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./components/dashboard.css";

export function DashboardView() {
  const [offerMode, setOfferMode] = useState<"voice" | "text">("voice");
  const [offerText, setOfferText] = useState("");
  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState<string | null>(null);
  const [selectedCustomerId, setSelectedCustomerId] = useState("");
  const { data, loading, error } = useDashboard();
  const location = useLocation();
  const navigate = useNavigate();
  const notice =
    location.state &&
    typeof location.state === "object" &&
    "notice" in location.state &&
    typeof location.state.notice === "string"
      ? location.state.notice
      : null;

  useEffect(() => {
    let cancelled = false;
    void getCustomers()
      .then((result) => {
        if (!cancelled) setCustomers(result);
      })
      .catch((customerError: unknown) => {
        if (!cancelled) {
          setCustomersError(
            customerError instanceof Error
              ? `Kunden konnten nicht geladen werden: ${customerError.message}`
              : "Kunden konnten nicht geladen werden.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setCustomersLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="dashboard-page wm-dashboard">
      {notice && <p className="wm-dashboard-feedback wm-dashboard-success" role="status">{notice}</p>}
      <header className="wm-dashboard-heading">
        <div><h1>Willkommen <span>zurück!</span></h1></div>
      </header>
      <section className="wm-offer-composer" aria-labelledby="wm-offer-title">
        <div className="wm-offer-intro">
          <div className="wm-offer-icon" aria-hidden="true"><Mic size={22} /></div>
          <div>
            <h2 id="wm-offer-title">Neues Angebot erstellen</h2>
            <p>Beschreibe deinen Auftrag. Wir erstellen dir daraus einen Angebotsentwurf.</p>
          </div>
        </div>
        <div className="wm-customer-select-row">
          <label htmlFor="wm-customer-select">Wähle einen Kunden aus</label>
          <div className="wm-customer-select-controls">
            <select id="wm-customer-select" value={selectedCustomerId} onChange={(event) => setSelectedCustomerId(event.target.value)}>
              <option value="">{customersLoading ? "Kunden werden geladen …" : "Kunde auswählen"}</option>
              <option value="draft">Ohne Kunde als Entwurf speichern</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {[customer.firstName, customer.lastName].filter(Boolean).join(" ") || customer.email}
                </option>
              ))}
            </select>
            <button className="wm-add-customer" type="button" onClick={() => navigate("/kunden/neu")}>
              <Plus size={16} aria-hidden="true" /> Kunde anlegen
            </button>
          </div>
        </div>
        {customersError && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{customersError}</p>}
        <div className="wm-offer-mode" role="group" aria-label="Eingabemethode">
          <button type="button" className={offerMode === "voice" ? "is-active" : ""} aria-pressed={offerMode === "voice"} onClick={() => setOfferMode("voice")}>Sprache</button>
          <button type="button" className={offerMode === "text" ? "is-active" : ""} aria-pressed={offerMode === "text"} onClick={() => setOfferMode("text")}>Text</button>
        </div>
        {offerMode === "voice" ? (
          <button className="wm-record-cta" type="button" onClick={() => navigate("/home", {
            state: {
              mode: "voice",
              customerId: selectedCustomerId && selectedCustomerId !== "draft"
                ? Number(selectedCustomerId)
                : null,
              saveAsDraft: selectedCustomerId === "draft",
            },
          })}>
            <Mic size={17} aria-hidden="true" /> Aufnahme starten
          </button>
        ) : (
          <div className="wm-dashboard-text">
            <textarea aria-label="Auftrag beschreiben" value={offerText} onChange={(event) => setOfferText(event.target.value)} placeholder="Gib hier deinen Text ein und die KI wird dir ein passendes Angebot erstellen." />
            <button type="button" onClick={() => navigate("/home", {
              state: {
                mode: "text",
                transcript: offerText,
                customerId: selectedCustomerId && selectedCustomerId !== "draft"
                  ? Number(selectedCustomerId)
                  : null,
                saveAsDraft: selectedCustomerId === "draft",
              },
            })}>Angebot erstellen</button>
          </div>
        )}
      </section>
      <DashboardOverview data={data} loading={loading} error={error} />
    </div>
  );
}
