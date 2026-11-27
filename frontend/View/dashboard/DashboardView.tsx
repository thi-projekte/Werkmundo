import { DashboardOverview } from "./components/DashboardOverview";
import { useDashboard } from "../../ViewModel/dashboard/legacy/useDashboard";
import { getCurrentUser, getCustomers, type CustomerProfile, type UserProfile } from "../../infrastructure/api/userApi";
import { getDocumentOffers } from "../../infrastructure/api/documentApi";
import type { OfferRecord } from "../../infrastructure/api/offerApi";
import { getStoredPlan, OFFER_PLANS } from "../../Model/subscription/plans";
import { ChevronDown, Mic, Plus } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./components/dashboard.css";

function profileName(profile: UserProfile | null): string {
  if (!profile) return "";
  return [profile.firstName, profile.lastName].filter(Boolean).join(" ");
}

export function DashboardView() {
  const [offerMode, setOfferMode] = useState<"voice" | "text">("voice");
  const [offerText, setOfferText] = useState("");
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [offers, setOffers] = useState<OfferRecord[]>([]);
  const [offersLoading, setOffersLoading] = useState(true);
  const [offersError, setOffersError] = useState(false);
  const [planId] = useState(getStoredPlan);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState<string | null>(null);
  const [selectedCustomerId, setSelectedCustomerId] = useState("");
  const [customerMenuOpen, setCustomerMenuOpen] = useState(false);
  const customerPickerRef = useRef<HTMLDivElement>(null);
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
  const selectedCustomer = customers.find((customer) => String(customer.id) === selectedCustomerId);
  const selectedCustomerLabel = selectedCustomer
    ? [selectedCustomer.firstName, selectedCustomer.lastName].filter(Boolean).join(" ") || selectedCustomer.email
    : selectedCustomerId === "draft"
      ? "Ohne Kunde als Entwurf speichern"
      : customersLoading
        ? "Kunden werden geladen …"
        : "Kunde auswählen";
  const currentPlan = OFFER_PLANS.find((plan) => plan.id === planId) ?? OFFER_PLANS[1];
  const currentMonth = new Date();
  const offersUsed = offers.filter((offer) => {
    if (!offer.createdAt) return false;
    const createdAt = new Date(offer.createdAt);
    return !Number.isNaN(createdAt.valueOf())
      && createdAt.getFullYear() === currentMonth.getFullYear()
      && createdAt.getMonth() === currentMonth.getMonth();
  }).length;

  useEffect(() => {
    if (!customerMenuOpen) return;
    const closeOnOutsideClick = (event: PointerEvent) => {
      if (event.target instanceof Node && !customerPickerRef.current?.contains(event.target)) {
        setCustomerMenuOpen(false);
      }
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setCustomerMenuOpen(false);
    };
    document.addEventListener("pointerdown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [customerMenuOpen]);

  useEffect(() => {
    let cancelled = false;
    void getCurrentUser()
      .then((result) => {
        if (!cancelled) setProfile(result);
      })
      .catch((profileFailure: unknown) => {
        if (!cancelled) {
          setProfileError(
            profileFailure instanceof Error
              ? `Profil konnte nicht geladen werden: ${profileFailure.message}`
              : "Profil konnte nicht geladen werden.",
          );
        }
      });

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

    void getDocumentOffers()
      .then((result) => {
        if (!cancelled) setOffers(result);
      })
      .catch((loadError: unknown) => {
        console.error("Angebotskontingent konnte nicht geladen werden:", loadError);
        if (!cancelled) setOffersError(true);
      })
      .finally(() => {
        if (!cancelled) setOffersLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="dashboard-page wm-dashboard">
      {notice && <p className="wm-dashboard-feedback wm-dashboard-success" role="status">{notice}</p>}
      <header className="wm-dashboard-heading">
        <div>
          <h1>Willkommen <span>{profileName(profile) ? `${profileName(profile)}.` : "zurück!"}</span></h1>
          {profileError && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{profileError}</p>}
        </div>
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
            <div className={`wm-customer-picker ${customerMenuOpen ? "is-open" : ""}`} ref={customerPickerRef}>
              <button
                id="wm-customer-select"
                className="wm-customer-picker-trigger"
                type="button"
                aria-haspopup="listbox"
                aria-expanded={customerMenuOpen}
                aria-controls="wm-customer-options"
                onClick={() => setCustomerMenuOpen((open) => !open)}
                onKeyDown={(event) => {
                  if (event.key === "ArrowDown") {
                    event.preventDefault();
                    setCustomerMenuOpen(true);
                  }
                }}
              >
                <span>{selectedCustomerLabel}</span>
                <ChevronDown size={17} aria-hidden="true" />
              </button>
              {customerMenuOpen && (
                <div className="wm-customer-picker-menu" id="wm-customer-options" role="listbox" aria-label="Kunde auswählen">
                  <button type="button" role="option" aria-selected={selectedCustomerId === "draft"} className={selectedCustomerId === "draft" ? "is-selected" : ""} onClick={() => { setSelectedCustomerId("draft"); setCustomerMenuOpen(false); }}>
                    Ohne Kunde als Entwurf speichern
                  </button>
                  {customers.map((customer) => {
                    const value = String(customer.id);
                    const label = [customer.firstName, customer.lastName].filter(Boolean).join(" ") || customer.email;
                    return (
                      <button key={customer.id} type="button" role="option" aria-selected={selectedCustomerId === value} className={selectedCustomerId === value ? "is-selected" : ""} onClick={() => { setSelectedCustomerId(value); setCustomerMenuOpen(false); }}>
                        {label}
                      </button>
                    );
                  })}
                  {!customersLoading && customers.length === 0 && <p className="wm-customer-picker-empty">Noch keine Kunden vorhanden.</p>}
                </div>
              )}
            </div>
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
      <DashboardOverview
        data={data}
        loading={loading}
        error={error}
        quota={{
          used: offersUsed,
          limit: currentPlan.offers,
          planLabel: currentPlan.label,
          loading: offersLoading,
          error: offersError,
          nextPlan: OFFER_PLANS[OFFER_PLANS.findIndex((plan) => plan.id === currentPlan.id) + 1],
        }}
      />
    </div>
  );
}
