import "../dashboard/components/dashboard.css";
import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { FileText, Search } from "lucide-react";
import {
  getDocumentOffers,
  getInvoices,
  type InvoiceRecord,
} from "../../infrastructure/api/documentApi";
import type { OfferRecord } from "../../infrastructure/api/offerApi";

type DocumentTab = "angebote" | "rechnungen";

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

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    VERSENDET: "Versendet",
    ANGENOMMEN: "Angenommen",
    ABGELEHNT: "Abgelehnt",
    ENTWURF: "Entwurf",
    IN_BEARBEITUNG: "In Bearbeitung",
  };
  return labels[status] ?? status.replace(/_/g, " ");
}

export function DocumentListView() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab: DocumentTab = searchParams.get("tab") === "rechnungen" ? "rechnungen" : "angebote";
  const [offers, setOffers] = useState<OfferRecord[]>([]);
  const [invoices, setInvoices] = useState<InvoiceRecord[]>([]);
  const [offersError, setOffersError] = useState<string | null>(null);
  const [invoicesError, setInvoicesError] = useState<string | null>(null);
  const [offersLoading, setOffersLoading] = useState(true);
  const [invoicesLoading, setInvoicesLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("alle");

  useEffect(() => {
    let active = true;
    void getDocumentOffers()
      .then((result) => {
        if (active) setOffers(result);
      })
      .catch((loadError: unknown) => {
        if (active) setOffersError(
          loadError instanceof Error
            ? `Angebote konnten nicht geladen werden: ${loadError.message}`
            : "Angebote konnten nicht geladen werden.",
        );
      })
      .finally(() => {
        if (active) setOffersLoading(false);
      });
    void getInvoices()
      .then((result) => {
        if (active) setInvoices(result);
      })
      .catch((loadError: unknown) => {
        if (active) setInvoicesError(
          loadError instanceof Error
            ? `Rechnungen konnten nicht geladen werden: ${loadError.message}`
            : "Rechnungen konnten nicht geladen werden.",
        );
      })
      .finally(() => {
        if (active) setInvoicesLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const filteredOffers = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("de");
    return offers.filter((offer) => {
      const matchesQuery = !query || [offer.title, offer.businessKey, offer.status]
        .filter(Boolean)
        .join(" ")
        .toLocaleLowerCase("de")
        .includes(query);
      const matchesStatus = statusFilter === "alle" || offer.status === statusFilter;
      return matchesQuery && matchesStatus;
    });
  }, [offers, search, statusFilter]);

  const filteredInvoices = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("de");
    return invoices.filter((invoice) =>
      !query || [
        invoice.rechnungsnummer,
        invoice.offerBusinessKey,
        invoice.kundendaten?.vorname,
        invoice.kundendaten?.nachname,
      ]
        .filter(Boolean)
        .join(" ")
        .toLocaleLowerCase("de")
        .includes(query),
    );
  }, [invoices, search]);

  const setTab = (nextTab: DocumentTab) => {
    setSearchParams(nextTab === "angebote" ? { tab: "angebote" } : { tab: "rechnungen" });
    setStatusFilter("alle");
  };

  const loading = tab === "angebote" ? offersLoading : invoicesLoading;
  const error = tab === "angebote" ? offersError : invoicesError;
  const isEmpty = tab === "angebote" ? filteredOffers.length === 0 : filteredInvoices.length === 0;

  return (
    <div className="dashboard-page wm-dashboard wm-document-page">
      <header className="wm-dashboard-heading">
        <div>
          <h1>Deine <span>Dokumente</span></h1>
          <p className="wm-dashboard-description">Angebote und Rechnungen an einem Ort.</p>
        </div>
      </header>

      <section className="wm-list-panel wm-document-panel">
        <div className="wm-document-tabs" role="tablist" aria-label="Dokumenttyp">
          <button type="button" role="tab" aria-selected={tab === "angebote"} className={tab === "angebote" ? "is-active" : ""} onClick={() => setTab("angebote")}>Angebote</button>
          <button type="button" role="tab" aria-selected={tab === "rechnungen"} className={tab === "rechnungen" ? "is-active" : ""} onClick={() => setTab("rechnungen")}>Rechnungen</button>
        </div>

        <div className="wm-document-toolbar">
          <label className="wm-search-field">
            <Search size={16} aria-hidden="true" />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Suche nach Name, Adresse, Rechnungsnummer, etc." />
          </label>
          {tab === "angebote" && (
            <label className="wm-filter-select">
              <span className="wm-sr-only">Angebote filtern</span>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                <option value="alle">Filter: Alle</option>
                <option value="ENTWURF">Entwürfe</option>
                <option value="VERSENDET">Versendet</option>
                <option value="ANGENOMMEN">Angenommen</option>
                <option value="ABGELEHNT">Abgelehnt</option>
              </select>
            </label>
          )}
        </div>

        {error && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{error}</p>}
        {loading ? (
          <p className="wm-dashboard-feedback">{tab === "angebote" ? "Angebote" : "Rechnungen"} werden geladen …</p>
        ) : error ? null : isEmpty ? (
          <div className="wm-empty-state">
            {search || statusFilter !== "alle"
              ? "Keine passenden Dokumente gefunden."
              : `Noch keine ${tab} vorhanden.`}
          </div>
        ) : tab === "angebote" ? (
          <ul className="wm-document-items">
            {filteredOffers.map((offer) => (
              <li key={offer.businessKey}>
                <Link to={`/angebote/${encodeURIComponent(offer.businessKey)}/bearbeiten`}>
                  <span><strong>{offer.title || `Angebot ${offer.businessKey}`}</strong><small>Erstellt am {formatDate(offer.createdAt)} · {statusLabel(offer.status)}</small></span>
                  <span className={`wm-document-status wm-status-${offer.status.toLocaleLowerCase("de")}`}>{statusLabel(offer.status)}</span>
                  <strong className="wm-document-amount">{formatPrice(offer.gesamtPreis ?? 0)}</strong>
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <ul className="wm-document-items">
            {filteredInvoices.map((invoice) => (
              <li key={invoice.id}>
                <div>
                  <span><strong>Rechnung {invoice.rechnungsnummer}</strong><small>{[invoice.kundendaten?.vorname, invoice.kundendaten?.nachname].filter(Boolean).join(" ") || invoice.kundendaten?.email || `Angebot ${invoice.offerBusinessKey}`} · erstellt am {formatDate(invoice.createdAt)}</small></span>
                  <span className="wm-document-icon"><FileText size={18} aria-hidden="true" /></span>
                  <strong className="wm-document-amount">{formatPrice(invoice.gesamtPreis)}</strong>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
