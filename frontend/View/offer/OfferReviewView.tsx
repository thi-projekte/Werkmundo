import "../dashboard/components/dashboard.css";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowDownToLine, ArrowLeft, FileText, Plus, Send, Trash2, UserRound } from "lucide-react";
import { getOffer, type OfferPosition, type OfferRecord } from "../../infrastructure/api/offerApi";
import { shareOffer } from "../../infrastructure/api/documentApi";

type EditablePosition = {
  key: string;
  bezeichnung: string;
  menge: string;
  einheit: string;
  einzelPreis: string;
};

function toEditablePosition(position: OfferPosition, index: number): EditablePosition {
  return {
    key: String(position.id ?? index),
    bezeichnung: position.bezeichnung ?? "",
    menge: String(position.menge ?? 1),
    einheit: position.einheit ?? "Stk.",
    einzelPreis: String(position.einzelPreis ?? ""),
  };
}

function parseAmount(value: string) {
  const parsed = Number(value.replace(",", "."));
  return Number.isFinite(parsed) ? parsed : 0;
}

function money(value: number) {
  return new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" }).format(value);
}

function isEditablePosition(value: unknown): value is EditablePosition {
  if (typeof value !== "object" || value === null) return false;
  const position = value as Record<string, unknown>;
  return typeof position.key === "string"
    && typeof position.bezeichnung === "string"
    && typeof position.menge === "string"
    && typeof position.einheit === "string"
    && typeof position.einzelPreis === "string";
}

export function OfferReviewView() {
  const { businessKey = "" } = useParams();
  const navigate = useNavigate();
  const [offer, setOffer] = useState<OfferRecord | null>(null);
  const [positions, setPositions] = useState<EditablePosition[]>([]);
  const [originalPositions, setOriginalPositions] = useState<EditablePosition[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    void getOffer(businessKey)
      .then((loadedOffer) => {
        if (!active) return;
        setOffer(loadedOffer);
        const serverPositions = (loadedOffer.positions ?? []).map(toEditablePosition);
        setOriginalPositions(serverPositions);
        const storageKey = `werkmundo-offer-draft-${businessKey}`;
        const savedDraft = sessionStorage.getItem(storageKey);
        if (!savedDraft) {
          setPositions(serverPositions);
          return;
        }
        try {
          const parsed: unknown = JSON.parse(savedDraft);
          if (!Array.isArray(parsed) || !parsed.every(isEditablePosition)) {
            throw new Error("Ungültiges Datenformat");
          }
          setPositions(parsed);
        } catch (draftError) {
          console.error("Der lokale Angebotsentwurf konnte nicht gelesen werden:", draftError);
          setPositions(serverPositions);
          setError("Der lokal gespeicherte Entwurf konnte nicht gelesen werden.");
        }
      })
      .catch((loadError: unknown) => {
        if (active) {
          setError(
            loadError instanceof Error
              ? `Angebot konnte nicht geladen werden: ${loadError.message}`
              : "Angebot konnte nicht geladen werden.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [businessKey]);

  const total = useMemo(
    () => positions.reduce((sum, position) => sum + parseAmount(position.menge) * parseAmount(position.einzelPreis), 0),
    [positions],
  );
  const hasUnsavedChanges = useMemo(
    () => JSON.stringify(positions) !== JSON.stringify(originalPositions),
    [positions, originalPositions],
  );

  const updatePosition = (key: string, field: keyof EditablePosition, value: string) => {
    setPositions((current) => current.map((position) =>
      position.key === key ? { ...position, [field]: value } : position,
    ));
    setNotice("");
  };

  const addPosition = () => {
    setPositions((current) => [
      ...current,
      {
        key: `new-${Date.now()}`,
        bezeichnung: "",
        menge: "1",
        einheit: "Stk.",
        einzelPreis: "0,00",
      },
    ]);
  };

  const saveDraft = () => {
    setError("");
    try {
      sessionStorage.setItem(`werkmundo-offer-draft-${businessKey}`, JSON.stringify(positions));
      setNotice("Änderungen wurden lokal in dieser Sitzung gespeichert.");
    } catch (saveError) {
      console.error("Angebotsentwurf konnte nicht gespeichert werden:", saveError);
      setError("Änderungen konnten nicht lokal gespeichert werden.");
    }
  };

  const send = async () => {
    if (hasUnsavedChanges) {
      setError("Deine Änderungen sind noch nicht mit dem Angebotsservice gespeichert und können deshalb nicht versendet werden.");
      return;
    }
    setSending(true);
    setError("");
    try {
      await shareOffer(businessKey);
      navigate("/angebot/versendet", { state: { businessKey } });
    } catch (sendError: unknown) {
      setError(
        sendError instanceof Error
          ? `Angebot konnte nicht versendet werden: ${sendError.message}`
          : "Angebot konnte nicht versendet werden.",
      );
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="dashboard-page wm-dashboard wm-offer-edit-page">
      <header className="wm-dashboard-heading">
        <div>
          <p className="wm-dashboard-kicker">Dashboard / Angebot bearbeiten</p>
          <h1>Angebot <span>bearbeiten</span></h1>
        </div>
        <Link className="wm-back-link" to="/dokumente?tab=angebote"><ArrowLeft size={15} aria-hidden="true" />Zurück zu Dokumenten</Link>
      </header>

      {notice && <p className="wm-dashboard-feedback wm-dashboard-success" role="status">{notice}</p>}
      {error && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{error}</p>}
      {loading ? (
        <section className="wm-list-panel"><p className="wm-dashboard-feedback">Angebot wird geladen …</p></section>
      ) : offer && (
        <>
          <section className="wm-offer-meta-card">
            <span className="wm-customer-summary-icon"><UserRound size={20} aria-hidden="true" /></span>
            <span><small>Kunde</small><strong>{offer.customerId || "Kein Kunde zugeordnet"}</strong></span>
            <span><small>Projekt/Betreff</small><strong>{offer.title || offer.businessKey}</strong></span>
            <span><small>Angebotsnummer</small><strong>{offer.businessKey}</strong></span>
            <span><small>Datum</small><strong>{offer.createdAt ? new Date(offer.createdAt).toLocaleDateString("de-DE") : "—"}</strong></span>
            <span><small>Gültig bis</small><strong>—</strong></span>
          </section>

          <section className="wm-offer-edit-grid">
            <div className="wm-offer-lines-panel">
              <div className="wm-section-heading">
                <h2><FileText size={17} aria-hidden="true" />Positionen</h2>
                <button className="wm-add-button" type="button" onClick={addPosition} aria-label="Position hinzufügen"><Plus size={19} /></button>
              </div>
              <div className="wm-offer-table-wrap">
                <table className="wm-offer-edit-table">
                  <thead><tr><th>Bezeichnung</th><th>Menge</th><th>Einheit</th><th>Einzelpreis</th><th>Gesamt</th><th><span className="wm-sr-only">Aktionen</span></th></tr></thead>
                  <tbody>
                    {positions.map((position) => {
                      const lineTotal = parseAmount(position.menge) * parseAmount(position.einzelPreis);
                      return (
                        <tr key={position.key}>
                          <td><input aria-label="Bezeichnung" value={position.bezeichnung} onChange={(event) => updatePosition(position.key, "bezeichnung", event.target.value)} /></td>
                          <td><input aria-label="Menge" inputMode="decimal" value={position.menge} onChange={(event) => updatePosition(position.key, "menge", event.target.value)} /></td>
                          <td><select aria-label="Einheit" value={position.einheit} onChange={(event) => updatePosition(position.key, "einheit", event.target.value)}><option>Stk.</option><option>Std.</option><option>m</option><option>m²</option></select></td>
                          <td><input aria-label="Einzelpreis in Euro" inputMode="decimal" value={position.einzelPreis} onChange={(event) => updatePosition(position.key, "einzelPreis", event.target.value)} /></td>
                          <td className="wm-offer-line-total">{money(lineTotal)}</td>
                          <td><button className="wm-icon-button wm-delete-position" type="button" onClick={() => setPositions((current) => current.filter((entry) => entry.key !== position.key))} aria-label="Position entfernen"><Trash2 size={15} /></button></td>
                        </tr>
                      );
                    })}
                    {!positions.length && <tr><td className="wm-empty-cell" colSpan={6}>Noch keine Positionen vorhanden. Füge eine Position hinzu.</td></tr>}
                  </tbody>
                </table>
              </div>
              <button className="wm-add-position-link" type="button" onClick={addPosition}><Plus size={14} aria-hidden="true" />Position hinzufügen</button>
            </div>

            <aside className="wm-offer-preview-panel">
              <h2><FileText size={17} aria-hidden="true" />Angebotsvorschau</h2>
              <div className="wm-offer-preview"><FileText size={30} aria-hidden="true" /><span>Die Angebotsvorschau wird erstellt, sobald dein Dokument bereit ist.</span></div>
              <p className="wm-preview-pagination">‹ Seite 1 von 1 ›</p>
              <div className="wm-offer-summary">
                <h3>Zusammenfassung</h3>
                <p><span>Zwischensumme</span><strong>{money(total)}</strong></p>
                <p><span>Material</span><strong>{money(total)}</strong></p>
                <p><span>Arbeitsleistung</span><strong>{money(0)}</strong></p>
                <p><span>MwSt. (19 %)</span><strong>{money(total * 0.19)}</strong></p>
                <p className="wm-offer-grand-total"><span>Gesamtbetrag</span><strong>{money(total * 1.19)}</strong></p>
              </div>
              <button className="wm-preview-download" type="button" disabled><ArrowDownToLine size={14} aria-hidden="true" />PDF herunterladen</button>
            </aside>
          </section>

          <div className="wm-offer-edit-actions">
            <Link className="wm-button wm-button-secondary" to="/dokumente?tab=angebote">Abbrechen</Link>
            <button className="wm-button wm-button-secondary" type="button" onClick={saveDraft}>Entwurf speichern</button>
            <button className="wm-button wm-button-primary" type="button" onClick={send} disabled={sending}>
              <Send size={15} aria-hidden="true" />{sending ? "Wird versendet …" : "Angebot versenden"}
            </button>
          </div>
          <p className="wm-profile-hint">Hinweis: Änderungen an Positionen werden aktuell nur für diese Sitzung lokal gespeichert.</p>
        </>
      )}
    </div>
  );
}