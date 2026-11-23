import "../dashboard/components/dashboard.css";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowDownToLine,
  ArrowLeft,
  FileText,
  Plus,
  Send,
  Trash2,
  UserRound,
} from "lucide-react";
import {
  getOffer,
  updateDraftPositions,
  type DraftPositionInput,
  type OfferPosition,
  type OfferRecord,
} from "../../infrastructure/api/offerApi";
import {
  fetchDocumentPdf,
  generateOfferDocument,
  previewOfferDocument,
  shareOffer,
  type InvoiceIssuer,
  type OfferDocumentSnapshot,
} from "../../infrastructure/api/documentApi";
import {
  getCurrentUser,
  getCustomer,
  type CustomerProfile,
  type UserProfile,
} from "../../infrastructure/api/userApi";

type EditablePosition = {
  key: string;
  bezeichnung: string;
  menge: string;
  einheit: string;
  einzelPreis: string;
};

type OfferDocumentParties = {
  issuer: InvoiceIssuer;
  customer: InvoiceIssuer;
  recipientEmail: string;
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

function parseAmount(value: string): number {
  const parsed = Number(value.trim().replace(",", "."));
  return Number.isFinite(parsed) ? parsed : 0;
}

function money(value: number): string {
  return new Intl.NumberFormat("de-DE", {
    style: "currency",
    currency: "EUR",
  }).format(value);
}

function partyDetails(profile: CustomerProfile | UserProfile): InvoiceIssuer {
  const companyEmail = "companyEmail" in profile ? profile.companyEmail : undefined;
  return {
    name: profile.companyName || [profile.firstName, profile.lastName].filter(Boolean).join(" "),
    address: [
      [profile.street, profile.houseNumber].filter(Boolean).join(" "),
      [profile.zipCode, profile.city].filter(Boolean).join(" "),
      profile.country,
    ].filter(Boolean).join(", "),
    email: companyEmail || profile.email,
  };
}

function createOfferSnapshot(
  offer: OfferRecord,
  positions: EditablePosition[],
  parties: OfferDocumentParties,
): OfferDocumentSnapshot {
  if (!offer.createdAt) throw new Error("Das Angebotsdatum fehlt.");

  const items = positions.map((position) => ({
    description: position.bezeichnung.trim(),
    quantity: Number(position.menge.trim().replace(",", ".")),
    unit: position.einheit.trim(),
    unitPrice: Number(position.einzelPreis.trim().replace(",", ".")),
  }));
  if (
    items.some((item) =>
      !item.description ||
      !item.unit ||
      !Number.isFinite(item.quantity) ||
      item.quantity <= 0 ||
      !Number.isFinite(item.unitPrice) ||
      item.unitPrice < 0
    )
  ) {
    throw new Error("Bitte prüfe die Angebotspositionen.");
  }

  const documentItems = items.map((item) => ({
    ...item,
    lineTotal: Number((item.quantity * item.unitPrice).toFixed(2)),
  }));
  const netAmount = Number(
    documentItems.reduce((sum, item) => sum + item.lineTotal, 0).toFixed(2),
  );
  const taxAmount = Number((netAmount * 0.19).toFixed(2));

  return {
    documentDate: offer.createdAt.slice(0, 10),
    issuer: parties.issuer,
    customer: parties.customer,
    items: documentItems,
    netAmount,
    taxLines: [{ label: "MwSt.", rate: 19, amount: taxAmount }],
    totalAmount: Number((netAmount + taxAmount).toFixed(2)),
    currency: "EUR",
    notes: items.length
      ? undefined
      : "ENTWURF: Keine Angebotspositionen vorhanden. Dieses Dokument ist nicht versandfertig.",
  };
}

export function OfferReviewView() {
  const { businessKey = "" } = useParams();
  const navigate = useNavigate();
  const [offer, setOffer] = useState<OfferRecord | null>(null);
  const [positions, setPositions] = useState<EditablePosition[]>([]);
  const [originalPositions, setOriginalPositions] = useState<EditablePosition[]>([]);
  const [documentParties, setDocumentParties] = useState<OfferDocumentParties | null>(null);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [saving, setSaving] = useState(false);
  const [generatingPdf, setGeneratingPdf] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [pdfUrl, setPdfUrl] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setOffer(null);
    setPositions([]);
    setOriginalPositions([]);
    setDocumentParties(null);
    setPdfUrl(null);
    setError("");
    setNotice("");

    void getOffer(businessKey)
      .then((loadedOffer) => {
        if (!active) return;
        const loadedPositions = (loadedOffer.positions ?? []).map(toEditablePosition);
        setOffer(loadedOffer);
        setPositions(loadedPositions);
        setOriginalPositions(loadedPositions);
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

  const customerId = offer?.customerId;

  useEffect(() => {
    let active = true;
    setDocumentParties(null);
    const numericCustomerId = Number(customerId);
    if (!customerId || !Number.isSafeInteger(numericCustomerId) || numericCustomerId < 1) {
      setPreviewLoading(false);
      return () => {
        active = false;
      };
    }

    void Promise.all([getCustomer(numericCustomerId), getCurrentUser()])
      .then(([customer, profile]) => {
        if (active) {
          setDocumentParties({
            issuer: partyDetails(profile),
            customer: partyDetails(customer),
            recipientEmail: customer.email,
          });
        }
      })
      .catch((loadError: unknown) => {
        if (active) {
          setError(
            loadError instanceof Error
              ? `Kunden- oder Absenderdaten konnten nicht geladen werden: ${loadError.message}`
              : "Kunden- oder Absenderdaten konnten nicht geladen werden.",
          );
        }
      });

    return () => {
      active = false;
    };
  }, [customerId]);

  useEffect(() => () => {
    if (pdfUrl) URL.revokeObjectURL(pdfUrl);
  }, [pdfUrl]);

  useEffect(() => {
    if (loading || !offer || !documentParties) return;
    let active = true;
    setPreviewLoading(true);
    const timeout = window.setTimeout(() => {
      try {
        const snapshot = createOfferSnapshot(offer, positions, documentParties);
        void previewOfferDocument(businessKey, snapshot)
          .then((pdf) => {
            if (active) {
              setPdfUrl(URL.createObjectURL(pdf));
              setError("");
            }
          })
          .catch((previewError: unknown) => {
            if (active) {
              setPdfUrl(null);
              setError(
                previewError instanceof Error
                  ? `Vorschau konnte nicht aktualisiert werden: ${previewError.message}`
                  : "Vorschau konnte nicht aktualisiert werden.",
              );
            }
          })
          .finally(() => {
            if (active) setPreviewLoading(false);
          });
      } catch (previewError: unknown) {
        if (active) {
          setPdfUrl(null);
          setError(
            previewError instanceof Error
              ? `Vorschau konnte nicht aktualisiert werden: ${previewError.message}`
              : "Vorschau konnte nicht aktualisiert werden.",
          );
          setPreviewLoading(false);
        }
      }
    }, 350);

    return () => {
      active = false;
      window.clearTimeout(timeout);
    };
  }, [businessKey, documentParties, loading, offer, positions]);

  const total = useMemo(
    () => positions.reduce(
      (sum, position) => sum + parseAmount(position.menge) * parseAmount(position.einzelPreis),
      0,
    ),
    [positions],
  );
  const hasUnsavedChanges = useMemo(
    () => JSON.stringify(positions) !== JSON.stringify(originalPositions),
    [positions, originalPositions],
  );
  const busy = saving || sending || generatingPdf;
  const editable = offer?.status === "ENTWURF" && !busy;

  const updatePosition = (key: string, field: keyof EditablePosition, value: string) => {
    setPositions((current) =>
      current.map((position) =>
        position.key === key ? { ...position, [field]: value } : position,
      ),
    );
    setNotice("");
  };

  const addPosition = () => {
    setPositions((current) => [
      ...current,
      {
        key: `new-${crypto.randomUUID()}`,
        bezeichnung: "",
        menge: "1",
        einheit: "Stk.",
        einzelPreis: "0,00",
      },
    ]);
    setNotice("");
  };

  const removePosition = (key: string) => {
    setPositions((current) => current.filter((position) => position.key !== key));
    setNotice("");
  };

  const saveDraft = async () => {
    if (busy) return;
    setError("");
    setNotice("");
    if (offer?.status !== "ENTWURF") {
      setError("Positionen können nur im Entwurfsstatus gespeichert werden.");
      return;
    }

    const inputs: DraftPositionInput[] = positions.map((position) => ({
      bezeichnung: position.bezeichnung.trim(),
      menge: Number(position.menge.trim().replace(",", ".")),
      einheit: position.einheit.trim(),
      einzelPreis: position.einzelPreis.trim()
        ? Number(position.einzelPreis.trim().replace(",", "."))
        : Number.NaN,
    }));
    if (
      inputs.some((position) =>
        !position.bezeichnung ||
        !position.einheit ||
        !Number.isFinite(position.menge) ||
        position.menge <= 0 ||
        !Number.isFinite(position.einzelPreis) ||
        position.einzelPreis < 0
      )
    ) {
      setError("Bitte prüfe Bezeichnung, Einheit, Menge und Nettoeinzelpreis.");
      return;
    }

    setSaving(true);
    try {
      const updatedOffer = await updateDraftPositions(businessKey, inputs);
      const savedPositions = (updatedOffer.positions ?? []).map(toEditablePosition);
      setOffer(updatedOffer);
      setPositions(savedPositions);
      setOriginalPositions(savedPositions);
      setNotice("Der Angebotsentwurf wurde gespeichert.");
    } catch (failure: unknown) {
      setError(failure instanceof Error ? failure.message : "Speichern fehlgeschlagen.");
    } finally {
      setSaving(false);
    }
  };

  const send = async () => {
    if (busy) return;
    if (!positions.length) {
      setError("Ein Angebot ohne Positionen kann nicht versendet werden.");
      return;
    }
    if (hasUnsavedChanges) {
      setError("Bitte speichere deine Änderungen, bevor du das Angebot versendest.");
      return;
    }

    setSending(true);
    setError("");
    setNotice("");
    try {
      await shareOffer(businessKey);
      navigate("/angebot/versendet", { state: { businessKey } });
    } catch (sendError: unknown) {
      setError(sendError instanceof Error ? `Angebot konnte nicht versendet werden: ${sendError.message}` : "Angebot konnte nicht versendet werden.");
    } finally {
      setSending(false);
    }
  };

  const downloadPdf = async () => {
    if (busy || !offer) return;
    setError("");
    setNotice("");
    const offerCustomerId = offer.customerId;
    if (
      !offerCustomerId ||
      !Number.isSafeInteger(Number(offerCustomerId)) ||
      Number(offerCustomerId) < 1
    ) {
      setError("Ordne dem Angebot zuerst einen Kunden zu.");
      return;
    }
    if (!documentParties) {
      setError("Kunden- oder Absenderdaten sind noch nicht geladen.");
      return;
    }

    setGeneratingPdf(true);
    try {
      const snapshot = createOfferSnapshot(offer, positions, documentParties);
      const pdf = positions.length
        ? await fetchDocumentPdf(
            (
              await generateOfferDocument(
                businessKey,
                snapshot,
                offerCustomerId,
                documentParties.recipientEmail,
              )
            ).documentId,
          )
        : await previewOfferDocument(businessKey, snapshot);
      const nextPdfUrl = URL.createObjectURL(pdf);
      setPdfUrl(nextPdfUrl);

      const link = window.document.createElement("a");
      link.href = nextPdfUrl;
      link.download = `angebot-${businessKey.replace(/[^A-Za-z0-9_-]/g, "_")}.pdf`;
      window.document.body.append(link);
      link.click();
      link.remove();
      setNotice("Das Angebots-PDF wurde heruntergeladen.");
    } catch (pdfError: unknown) {
      setError(pdfError instanceof Error ? `PDF konnte nicht erstellt werden: ${pdfError.message}` : "PDF konnte nicht erstellt werden.");
    } finally {
      setGeneratingPdf(false);
    }
  };

  return (
    <div className="dashboard-page wm-dashboard wm-offer-edit-page">
      <header className="wm-dashboard-heading">
        <div>
          <p className="wm-dashboard-kicker">Dashboard / Angebot bearbeiten</p>
          <h1>Angebot <span>bearbeiten</span></h1>
        </div>
        <Link className="wm-back-link" to="/dokumente?tab=angebote">
          <ArrowLeft size={15} aria-hidden="true" />
          Zurück zu Dokumenten
        </Link>
      </header>

      {notice && <p className="wm-dashboard-feedback wm-dashboard-success" role="status">{notice}</p>}
      {error && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{error}</p>}

      {loading ? (
        <section className="wm-list-panel">
          <p className="wm-dashboard-feedback">Angebot wird geladen …</p>
        </section>
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
                <button className="wm-add-button" type="button" onClick={addPosition} disabled={!editable} aria-label="Position hinzufügen">
                  <Plus size={19} />
                </button>
              </div>
              <div className="wm-offer-table-wrap">
                <table className="wm-offer-edit-table">
                  <thead>
                    <tr>
                      <th>Bezeichnung</th><th>Menge</th><th>Einheit</th>
                      <th>Einzelpreis</th><th>Gesamt</th><th><span className="wm-sr-only">Aktionen</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {positions.map((position) => {
                      const lineTotal = parseAmount(position.menge) * parseAmount(position.einzelPreis);
                      return (
                        <tr key={position.key}>
                          <td><input aria-label="Bezeichnung" value={position.bezeichnung} disabled={!editable} onChange={(event) => updatePosition(position.key, "bezeichnung", event.target.value)} /></td>
                          <td><input aria-label="Menge" inputMode="decimal" value={position.menge} disabled={!editable} onChange={(event) => updatePosition(position.key, "menge", event.target.value)} /></td>
                          <td>
                            <select aria-label="Einheit" value={position.einheit} disabled={!editable} onChange={(event) => updatePosition(position.key, "einheit", event.target.value)}>
                              {!["Stk.", "Std.", "m", "m²"].includes(position.einheit) && <option value={position.einheit}>{position.einheit}</option>}
                              <option>Stk.</option><option>Std.</option><option>m</option><option>m²</option>
                            </select>
                          </td>
                          <td><input aria-label="Nettoeinzelpreis in Euro" inputMode="decimal" value={position.einzelPreis} disabled={!editable} onChange={(event) => updatePosition(position.key, "einzelPreis", event.target.value)} /></td>
                          <td className="wm-offer-line-total">{money(lineTotal)}</td>
                          <td><button className="wm-icon-button wm-delete-position" type="button" disabled={!editable} onClick={() => removePosition(position.key)} aria-label="Position entfernen"><Trash2 size={15} /></button></td>
                        </tr>
                      );
                    })}
                    {!positions.length && <tr><td className="wm-empty-cell" colSpan={6}>Noch keine Positionen vorhanden. Füge eine Position hinzu.</td></tr>}
                  </tbody>
                </table>
              </div>
              <button className="wm-add-position-link" type="button" onClick={addPosition} disabled={!editable}>
                <Plus size={14} aria-hidden="true" />Position hinzufügen
              </button>
            </div>

            <aside className="wm-offer-preview-panel">
              <h2><FileText size={17} aria-hidden="true" />Angebotsvorschau</h2>
              <div className={`wm-offer-preview${pdfUrl ? " wm-offer-preview-pdf" : ""}`}>
                {pdfUrl ? (
                  <iframe
                    title="Angebotsvorschau"
                    src={`${pdfUrl}#toolbar=0&navpanes=0&scrollbar=0&view=Fit`}
                  />
                ) : (
                  <>
                    <FileText size={30} aria-hidden="true" />
                    <span>{previewLoading ? "PDF-Vorschau wird aktualisiert …" : "Die PDF-Vorschau erscheint hier."}</span>
                  </>
                )}
              </div>
              {!positions.length && <p className="wm-dashboard-feedback" role="note">Leeres PDF ist möglich; ein Versand ohne Positionen bleibt gesperrt.</p>}
              <p className="wm-preview-pagination">‹ Seite 1 von 1 ›</p>
              <div className="wm-offer-summary">
                <h3>Zusammenfassung</h3>
                <p><span>Zwischensumme</span><strong>{money(total)}</strong></p>
                <p><span>Material</span><strong>{money(total)}</strong></p>
                <p><span>Arbeitsleistung</span><strong>{money(0)}</strong></p>
                <p><span>MwSt. (19 %)</span><strong>{money(total * 0.19)}</strong></p>
                <p className="wm-offer-grand-total"><span>Gesamtbetrag</span><strong>{money(total * 1.19)}</strong></p>
              </div>
              <button className="wm-preview-download" type="button" onClick={() => void downloadPdf()} disabled={busy}>
                <ArrowDownToLine size={14} aria-hidden="true" />
                {generatingPdf ? "PDF wird erstellt …" : "PDF herunterladen"}
              </button>
            </aside>
          </section>

          <div className="wm-offer-edit-actions">
            <Link className="wm-button wm-button-secondary" to="/dokumente?tab=angebote">Abbrechen</Link>
            <button className="wm-button wm-button-secondary" type="button" onClick={() => void saveDraft()} disabled={!editable}>
              {saving ? "Wird gespeichert …" : "Entwurf speichern"}
            </button>
            <button className="wm-button wm-button-primary" type="button" onClick={() => void send()} disabled={busy || positions.length === 0}>
              <Send size={15} aria-hidden="true" />
              {sending ? "Wird versendet …" : "Angebot versenden"}
            </button>
          </div>
          <p className="wm-profile-hint">Änderungen werden mit „Entwurf speichern“ dauerhaft gespeichert.</p>
        </>
      )}
    </div>
  );
}
