import "../dashboard/components/dashboard.css";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft, FileText, Plus, UserRound } from "lucide-react";
import {
  getCustomer,
  type CustomerProfile,
} from "../../infrastructure/api/userApi";
import {
  createInvoice,
  getDocumentOffers,
  getInvoices,
  type InvoiceRecord,
} from "../../infrastructure/api/documentApi";
import type { OfferRecord } from "../../infrastructure/api/offerApi";

function customerName(customer: CustomerProfile) {
  return [customer.firstName, customer.lastName].filter(Boolean).join(" ") || customer.email;
}

function formatDate(value?: string) {
  if (!value) return "Datum nicht verfügbar";
  const date = new Date(value);
  return Number.isNaN(date.valueOf()) ? "Datum nicht verfügbar" : date.toLocaleDateString("de-DE");
}

function formatPrice(value: number | string) {
  const amount = typeof value === "number" ? value : Number(value);
  return Number.isFinite(amount)
    ? new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" }).format(amount)
    : "—";
}

export function CustomerDetailView() {
  const { customerId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const tab = searchParams.get("tab") === "rechnungen" ? "rechnungen" : "angebote";
  const [customer, setCustomer] = useState<CustomerProfile | null>(null);
  const [offers, setOffers] = useState<OfferRecord[]>([]);
  const [invoices, setInvoices] = useState<InvoiceRecord[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [offersError, setOffersError] = useState<string | null>(null);
  const [invoicesError, setInvoicesError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [creatingInvoice, setCreatingInvoice] = useState(false);
  const [invoiceActionError, setInvoiceActionError] = useState("");

  useEffect(() => {
    const id = Number(customerId);
    if (!Number.isSafeInteger(id) || id < 1) {
      setError("Die Kunden-ID ist ungültig.");
      setLoading(false);
      return;
    }

    let active = true;
    void Promise.allSettled([getCustomer(id), getDocumentOffers(), getInvoices()])
      .then(([customerResult, offersResult, invoicesResult]) => {
        if (!active) return;
        if (customerResult.status === "fulfilled") {
          setCustomer(customerResult.value);
        } else {
          setError(
            customerResult.reason instanceof Error
              ? `Kundendaten konnten nicht geladen werden: ${customerResult.reason.message}`
              : "Kundendaten konnten nicht geladen werden.",
          );
        }
        if (offersResult.status === "fulfilled") {
          setOffers(offersResult.value);
        } else {
          setOffersError(
            offersResult.reason instanceof Error
              ? `Angebote konnten nicht geladen werden: ${offersResult.reason.message}`
              : "Angebote konnten nicht geladen werden.",
          );
        }
        if (invoicesResult.status === "fulfilled") {
          setInvoices(invoicesResult.value);
        } else {
          setInvoicesError(
            invoicesResult.reason instanceof Error
              ? `Rechnungen konnten nicht geladen werden: ${invoicesResult.reason.message}`
              : "Rechnungen konnten nicht geladen werden.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [customerId]);

  const customerOffers = useMemo(
    () => offers.filter((offer) => offer.customerId === customerId),
    [offers, customerId],
  );
  const offerKeys = useMemo(
    () => new Set(customerOffers.map((offer) => offer.businessKey)),
    [customerOffers],
  );
  const customerInvoices = invoices.filter((invoice) => offerKeys.has(invoice.offerBusinessKey));
  const currentItems = tab === "angebote" ? customerOffers : customerInvoices;
  const billableOffer = customerOffers.find((offer) => offer.status === "ANGENOMMEN");

  const createCustomerInvoice = async () => {
    if (!billableOffer) return;
    setCreatingInvoice(true);
    setInvoiceActionError("");
    try {
      const invoice = await createInvoice(billableOffer.businessKey);
      setInvoices((current) => [invoice, ...current]);
    } catch (createError: unknown) {
      setInvoiceActionError(
        createError instanceof Error
          ? `Rechnung konnte nicht erstellt werden: ${createError.message}`
          : "Rechnung konnte nicht erstellt werden.",
      );
    } finally {
      setCreatingInvoice(false);
    }
  };

  return (
    <div className="dashboard-page wm-dashboard wm-customer-detail">
      <header className="wm-dashboard-heading">
        <div>
          <h1>Deine <span>Kunden</span></h1>
          <p className="wm-dashboard-description">Kundendaten und Dokumente im Überblick.</p>
        </div>
      </header>

      {error && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{error}</p>}
      {loading ? (
        <section className="wm-list-panel"><p className="wm-dashboard-feedback">Kunde wird geladen …</p></section>
      ) : customer && (
        <>
          <section className="wm-customer-summary">
            <div className="wm-customer-summary-icon"><UserRound size={21} aria-hidden="true" /></div>
            <div>
              <h2>{customerName(customer)}</h2>
              <p>{[customer.street, customer.houseNumber].filter(Boolean).join(" ")}{customer.city ? `, ${[customer.zipCode, customer.city].filter(Boolean).join(" ")}` : ""}{customer.country ? `, ${customer.country}` : ""}</p>
              <p>{customer.email}{customer.phoneNumber ? ` · ${customer.phoneNumber}` : ""}</p>
            </div>
            <Link className="wm-text-button" to={`/kunden/${customer.id}/bearbeiten`}>Bearbeiten</Link>
          </section>

          <section className="wm-list-panel wm-customer-document-panel">
            <div className="wm-customer-tabs" role="tablist" aria-label="Kundendokumente">
              <button type="button" role="tab" aria-selected={tab === "angebote"} className={tab === "angebote" ? "is-active" : ""} onClick={() => setSearchParams({ tab: "angebote" })}>Angebote</button>
              <button type="button" role="tab" aria-selected={tab === "rechnungen"} className={tab === "rechnungen" ? "is-active" : ""} onClick={() => setSearchParams({ tab: "rechnungen" })}>Rechnungen</button>
            </div>
            {(tab === "angebote" ? offersError : invoicesError) ? (
              <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
                {tab === "angebote" ? offersError : invoicesError}
              </p>
            ) : currentItems.length ? (
              <ul className="wm-customer-document-items">
                {currentItems.map((item) => {
                  const isOffer = "businessKey" in item;
                  const key = isOffer ? item.businessKey : item.rechnungsnummer;
                  const title = isOffer
                    ? item.title || `Angebot ${item.businessKey}`
                    : `Rechnung ${item.rechnungsnummer}`;
                  const date = item.createdAt;
                  const price = item.gesamtPreis;
                  return (
                    <li key={key}>
                      {isOffer ? (
                        <Link to={`/angebote/${encodeURIComponent(item.businessKey)}/bearbeiten`}>
                          <span className="wm-document-tile"><FileText size={17} aria-hidden="true" /></span>
                          <span><strong>{title}</strong><small>{formatDate(date)} · {item.status}</small></span>
                          <strong className="wm-document-amount">{formatPrice(price ?? 0)}</strong>
                        </Link>
                      ) : (
                        <div>
                          <span className="wm-document-tile"><FileText size={17} aria-hidden="true" /></span>
                          <span><strong>{title}</strong><small>{formatDate(date)}</small></span>
                          <strong className="wm-document-amount">{formatPrice(price ?? 0)}</strong>
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            ) : (
              <div className="wm-empty-state">
                Noch keine {tab === "angebote" ? "Angebote" : "Rechnungen"} für {customerName(customer)} vorhanden.
              </div>
            )}
            {invoiceActionError && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{invoiceActionError}</p>}
            <button
              className="wm-customer-primary-action"
              type="button"
              onClick={() => tab === "angebote"
                ? navigate("/home", { state: { mode: "voice", customerId: customer.id } })
                : void createCustomerInvoice()}
              disabled={tab === "rechnungen" && (!billableOffer || creatingInvoice)}
            >
              <Plus size={16} aria-hidden="true" />
              {tab === "angebote"
                ? "Neues Angebot erstellen"
                : creatingInvoice
                  ? "Rechnung wird erstellt …"
                  : billableOffer
                    ? `Neue Rechnung für ${customerName(customer)} erstellen`
                    : "Keine angenommenen Angebote"}
            </button>
            {tab === "rechnungen" && !billableOffer && !invoicesError && (
              <p className="wm-profile-hint">Eine Rechnung kann erst aus einem angenommenen Angebot erstellt werden.</p>
            )}
          </section>
          <Link className="wm-back-link" to="/kunden"><ArrowLeft size={15} aria-hidden="true" />Zurück zu deinen Kunden</Link>
        </>
      )}
    </div>
  );
}
