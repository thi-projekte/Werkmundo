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
  type OfferPosition,
  type OfferRecord,
} from "../../infrastructure/api/offerApi";
import {
  getDemoOfferPdf,
  getOfferPdf,
  shareOffer,
  type OfferPdfDraft,
} from "../../infrastructure/api/documentApi";
import { getCustomer, type CustomerProfile } from "../../infrastructure/api/userApi";
import { COMPANY_SETUP_STORAGE_KEY } from "../../Model/onboarding/setupForms";

type EditablePosition = {
  key: string;
  bezeichnung: string;
  menge: string;
  einheit: string;
  einzelPreis: string;
};

function toEditablePosition(
  position: OfferPosition,
  index: number,
): EditablePosition {
  return {
    key: String(position.id ?? index),
    bezeichnung: position.bezeichnung ?? "",
    menge: position.menge == null ? "" : String(position.menge),
    einheit: position.einheit ?? "",
    einzelPreis: String(position.einzelPreis ?? ""),
  };
}

function parseAmount(value: string): number {
  const parsed = Number(value.trim().replace(",", "."));
  return Number.isFinite(parsed) ? parsed : 0;
}

function validAmount(value: string, mustBePositive: boolean): boolean {
  if (!value.trim()) return false;
  const parsed = Number(value.trim().replace(",", "."));
  return Number.isFinite(parsed) && (mustBePositive ? parsed > 0 : parsed >= 0);
}

function money(value: number): string {
  return new Intl.NumberFormat("de-DE", {
    style: "currency",
    currency: "EUR",
  }).format(value);
}

function readSellerDetails(): OfferPdfDraft["seller"] | null {
  try {
    const stored = sessionStorage.getItem(COMPANY_SETUP_STORAGE_KEY);
    if (!stored) return null;
    const parsed: unknown = JSON.parse(stored);
    if (!parsed || typeof parsed !== "object") return null;

    const record = parsed as Record<string, unknown>;
    const field = (name: string) =>
      typeof record[name] === "string" ? record[name] as string : "";
    return {
      companyName: field("companyName"),
      contact: field("contact"),
      email: field("email"),
      street: field("street"),
      postalCode: field("postalCode"),
      city: field("city"),
    };
  } catch (error) {
    console.error("Die Unternehmensdaten für die Angebotsvorschau konnten nicht gelesen werden:", error);
    return null;
  }
}

function customerName(customer: CustomerProfile | null): string {
  if (!customer) return "";
  return customer.companyName?.trim()
    || [customer.firstName, customer.lastName].filter(Boolean).join(" ").trim();
}

function hasRequiredCustomerDetails(customer: CustomerProfile | null): boolean {
  return Boolean(
    customer
    && customerName(customer)
    && customer.email?.trim()
    && customer.street?.trim()
    && customer.zipCode?.trim()
    && customer.city?.trim(),
  );
}

export function OfferReviewView() {
  const { businessKey = "" } = useParams();
  const navigate = useNavigate();

  const [seller] = useState(readSellerDetails);
  const [offer, setOffer] = useState<OfferRecord | null>(null);
  const [customer, setCustomer] = useState<CustomerProfile | null>(null);
  const [customerLoading, setCustomerLoading] = useState(false);
  const [customerError, setCustomerError] = useState("");
  const [positions, setPositions] = useState<EditablePosition[]>([]);
  const [originalPositions, setOriginalPositions] = useState<
    EditablePosition[]
  >([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [saving, setSaving] = useState(false);
  const [pdfUrl, setPdfUrl] = useState("");
  const [pdfLoading, setPdfLoading] = useState(false);
  const [pdfError, setPdfError] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;

    setLoading(true);
    setOffer(null);
    setPositions([]);
    setOriginalPositions([]);
    setError("");
    setNotice("");

    const loadOffer = async () => {
      try {
        let loadedOffer = await getOffer(businessKey);
        for (let attempt = 0; attempt < 60
          && !loadedOffer.positions?.length
          && ["ERFASST", "IN_BEARBEITUNG"].includes(loadedOffer.status);
          attempt++) {
          setNotice("Dein Angebot wird aus der Beschreibung erstellt …");
          await new Promise((resolve) => window.setTimeout(resolve, 1000));
          if (!active) return;
          loadedOffer = await getOffer(businessKey);
        }
        if (!active) return;

        const serverPositions = (loadedOffer.positions ?? []).map(toEditablePosition);
        setOffer(loadedOffer);
        setPositions(serverPositions);
        setOriginalPositions(serverPositions);
        if (!serverPositions.length) {
          setNotice("Die KI-Erstellung läuft noch. Bitte lade die Seite in Kürze erneut.");
        }
      } catch (loadError: unknown) {
        if (!active) return;

        setError(
          loadError instanceof Error
            ? `Angebot konnte nicht geladen werden: ${loadError.message}`
            : "Angebot konnte nicht geladen werden.",
        );
      } finally {
        if (active) setLoading(false);
      }
    };

    void loadOffer();

    return () => {
      active = false;
    };
  }, [businessKey]);

  useEffect(() => {
    let active = true;
    const customerId = Number(offer?.customerId);
    if (!offer?.customerId || !Number.isSafeInteger(customerId) || customerId <= 0) {
      setCustomer(null);
      setCustomerError(
        offer?.customerId ? "Die Kundennummer ist ungültig." : "",
      );
      setCustomerLoading(false);
      return () => {
        active = false;
      };
    }

    setCustomerLoading(true);
    setCustomerError("");
    void getCustomer(customerId)
      .then((loadedCustomer) => {
        if (active) setCustomer(loadedCustomer);
      })
      .catch((loadError: unknown) => {
        if (!active) return;
        setCustomer(null);
        setCustomerError(
          loadError instanceof Error
            ? `Kundendaten konnten nicht geladen werden: ${loadError.message}`
            : "Kundendaten konnten nicht geladen werden.",
        );
      })
      .finally(() => {
        if (active) setCustomerLoading(false);
      });

    return () => {
      active = false;
    };
  }, [offer?.customerId]);

  const total = useMemo(
    () =>
      positions.reduce(
        (sum, position) =>
          sum +
          parseAmount(position.menge) * parseAmount(position.einzelPreis),
        0,
      ),
    [positions],
  );

  const hasUnsavedChanges = useMemo(
    () => JSON.stringify(positions) !== JSON.stringify(originalPositions),
    [positions, originalPositions],
  );

  const sellerComplete = Boolean(
    seller?.companyName?.trim()
    && seller.email?.trim()
    && seller.street?.trim()
    && seller.postalCode?.trim()
    && seller.city?.trim(),
  );
  const customerComplete = hasRequiredCustomerDetails(customer);
  const positionsComplete = positions.length > 0 && positions.every((position) =>
    Boolean(
      position.bezeichnung.trim()
      && position.einheit.trim()
      && validAmount(position.einzelPreis, false)
      && validAmount(position.menge, true),
    ),
  );
  const isDemo = Boolean(
    !offer?.customerId || !customerComplete || !sellerComplete || !positionsComplete,
  );
  const pdfDraft = useMemo<OfferPdfDraft | null>(() => {
    if (!offer || !positions.length) return null;
    const showCustomerDetails = customerComplete && customer !== null;
    return {
      businessKey: offer.businessKey,
      ...(offer.title ? { title: offer.title } : {}),
      customer: {
        name: showCustomerDetails ? customerName(customer) : "Musterkunde - Demo-Vorschau",
        ...(showCustomerDetails && customer
          ? {
              street: customer.street ?? "",
              postalCode: customer.zipCode ?? "",
              city: customer.city ?? "",
              email: customer.email ?? "",
            }
          : {}),
      },
      ...(seller ? { seller } : {}),
      positions: positions.map((position) => ({
        bezeichnung: position.bezeichnung,
        menge: position.menge,
        einheit: position.einheit,
        einzelPreis: position.einzelPreis.trim() || null,
      })),
    };
  }, [customer, customerComplete, offer, positions, seller]);

  useEffect(() => {
    let active = true;
    let objectUrl = "";
    if (
      !pdfDraft
      || loading
      || customerLoading
      || Boolean(offer?.customerId && !customer && !customerError)
    ) {
      setPdfUrl("");
      setPdfError("");
      setPdfLoading(false);
      return () => {
        active = false;
      };
    }

    setPdfUrl("");
    setPdfError("");
    setPdfLoading(true);
    const pdfRequest = isDemo
      ? getDemoOfferPdf(pdfDraft)
      : getOfferPdf(businessKey, pdfDraft);
    void pdfRequest
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        if (active) {
          objectUrl = url;
          setPdfUrl(url);
        } else {
          URL.revokeObjectURL(url);
        }
      })
      .catch((generationError: unknown) => {
        if (!active) return;
        setPdfError(
          generationError instanceof Error
            ? generationError.message
            : "Die PDF-Vorschau konnte nicht erstellt werden.",
        );
      })
      .finally(() => {
        if (active) setPdfLoading(false);
      });

    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [businessKey, customer, customerError, customerLoading, isDemo, loading, offer?.customerId, pdfDraft]);

  const busy = saving || sending;
  const editable = offer?.status === "ENTWURF" && !busy;

  const updatePosition = (
    key: string,
    field: keyof EditablePosition,
    value: string,
  ) => {
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
        menge: "",
        einheit: "",
        einzelPreis: "",
      },
    ]);
    setNotice("");
  };

  const removePosition = (key: string) => {
    setPositions((current) =>
      current.filter((position) => position.key !== key),
    );
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

    const inputs = positions.map((position) => ({
      bezeichnung: position.bezeichnung.trim(),
      menge: Number(position.menge.trim().replace(",", ".")),
      einheit: position.einheit.trim(),
      einzelPreis: position.einzelPreis.trim()
        ? Number(position.einzelPreis.trim().replace(",", "."))
        : Number.NaN,
    }));

    if (
      inputs.some(
        (position) =>
          !position.bezeichnung ||
          !position.einheit ||
          !Number.isFinite(position.menge) ||
          position.menge <= 0 ||
          !Number.isFinite(position.einzelPreis) ||
          position.einzelPreis < 0,
      )
    ) {
      setError(
        "Bitte prüfe Bezeichnung, Einheit, positive Mengen und gültige Nettoeinzelpreise.",
      );
      return;
    }

    setSaving(true);

    try {
      const updatedOffer = await updateDraftPositions(businessKey, inputs);
      const savedPositions = (updatedOffer.positions ?? []).map(
        toEditablePosition,
      );

      setOffer(updatedOffer);
      setPositions(savedPositions);
      setOriginalPositions(savedPositions);
      setNotice("Der Angebotsentwurf wurde gespeichert.");
    } catch (failure: unknown) {
      setError(
        failure instanceof Error
          ? failure.message
          : "Der Angebotsentwurf konnte nicht gespeichert werden.",
      );
    } finally {
      setSaving(false);
    }
  };

  const send = async () => {
    if (busy) return;

    if (isDemo) {
      setError("Demo-Angebote können nicht versendet werden. Ergänze die fehlenden Daten für ein reguläres Angebot.");
      return;
    }

    if (hasUnsavedChanges) {
      setError(
        "Bitte speichere deine Änderungen, bevor du das Angebot versendest.",
      );
      return;
    }

    setSending(true);
    setError("");
    setNotice("");

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
          <p className="wm-dashboard-kicker">
            Dashboard / Angebot bearbeiten
          </p>
          <h1>
            Angebot <span>bearbeiten</span>
          </h1>
        </div>
        <Link
          className="wm-back-link"
          to="/dokumente?tab=angebote"
        >
          <ArrowLeft size={15} aria-hidden="true" />
          Zurück zu Dokumenten
        </Link>
      </header>

      {notice && (
        <p
          className="wm-dashboard-feedback wm-dashboard-success"
          role="status"
        >
          {notice}
        </p>
      )}

      {error && (
        <p
          className="wm-dashboard-feedback wm-dashboard-error"
          role="alert"
        >
          {error}
        </p>
      )}

      {loading ? (
        <section className="wm-list-panel">
          <p className="wm-dashboard-feedback">
            Angebot wird geladen …
          </p>
        </section>
      ) : offer && (
        <>
          <section className="wm-offer-meta-card">
            <span className="wm-customer-summary-icon">
              <UserRound size={20} aria-hidden="true" />
            </span>
            <span>
              <small>Kunde</small>
              <strong>
                {isDemo
                  ? !offer.customerId || !customerComplete
                    ? "Musterkunde – Demo-Vorschau"
                    : customerName(customer)
                  : customerName(customer)}
              </strong>
            </span>
            <span>
              <small>Projekt/Betreff</small>
              <strong>{offer.title || offer.businessKey}</strong>
            </span>
            <span>
              <small>Angebotsnummer</small>
              <strong>{offer.businessKey}</strong>
            </span>
            <span>
              <small>Datum</small>
              <strong>
                {offer.createdAt
                  ? new Date(offer.createdAt).toLocaleDateString("de-DE")
                  : "—"}
              </strong>
            </span>
            <span>
              <small>Gültig bis</small>
              <strong>—</strong>
            </span>
          </section>

          <section className="wm-offer-edit-grid">
            <div className="wm-offer-lines-panel">
              <div className="wm-section-heading">
                <h2>
                  <FileText size={17} aria-hidden="true" />
                  Positionen
                </h2>
                <button
                  className="wm-add-button"
                  type="button"
                  onClick={addPosition}
                  disabled={!editable}
                  aria-label="Position hinzufügen"
                >
                  <Plus size={19} />
                </button>
              </div>

              <div className="wm-offer-table-wrap">
                <table className="wm-offer-edit-table">
                  <thead>
                    <tr>
                      <th>Bezeichnung</th>
                      <th>Menge</th>
                      <th>Einheit</th>
                      <th>Einzelpreis</th>
                      <th>Gesamt</th>
                      <th>
                        <span className="wm-sr-only">Aktionen</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {positions.map((position) => {
                      const hasValidQuantity = validAmount(position.menge, true);
                      const hasValidPrice = validAmount(position.einzelPreis, false);
                      const lineTotal =
                        parseAmount(position.menge) *
                        parseAmount(position.einzelPreis);

                      return (
                        <tr key={position.key}>
                          <td>
                            <input
                              aria-label="Bezeichnung"
                              value={position.bezeichnung}
                              disabled={!editable}
                              onChange={(event) =>
                                updatePosition(
                                  position.key,
                                  "bezeichnung",
                                  event.target.value,
                                )
                              }
                            />
                          </td>
                          <td>
                            <input
                              aria-label="Menge"
                              inputMode="decimal"
                              value={position.menge}
                              disabled={!editable}
                              onChange={(event) =>
                                updatePosition(
                                  position.key,
                                  "menge",
                                  event.target.value,
                                )
                              }
                            />
                          </td>
                          <td>
                            <select
                              aria-label="Einheit"
                              value={position.einheit}
                              disabled={!editable}
                              onChange={(event) =>
                                updatePosition(
                                  position.key,
                                  "einheit",
                                  event.target.value,
                                )
                              }
                            >
                              <option value="">Einheit wählen</option>
                              {!["Stk.", "Std.", "m", "m²"].includes(
                                position.einheit,
                              ) && (
                                <option value={position.einheit}>
                                  {position.einheit}
                                </option>
                              )}
                              <option>Stk.</option>
                              <option>Std.</option>
                              <option>m</option>
                              <option>m²</option>
                            </select>
                          </td>
                          <td>
                            <input
                              aria-label="Nettoeinzelpreis in Euro"
                              inputMode="decimal"
                              value={position.einzelPreis}
                              disabled={!editable}
                              onChange={(event) =>
                                updatePosition(
                                  position.key,
                                  "einzelPreis",
                                  event.target.value,
                                )
                              }
                            />
                          </td>
                          <td className="wm-offer-line-total">
                            {!hasValidPrice
                              ? "Preis offen"
                              : !hasValidQuantity
                                ? "Menge prüfen"
                                : `${isDemo ? "Beispiel: " : ""}${money(lineTotal)}`}
                          </td>
                          <td>
                            <button
                              className="wm-icon-button wm-delete-position"
                              type="button"
                              disabled={!editable}
                              onClick={() => removePosition(position.key)}
                              aria-label="Position entfernen"
                            >
                              <Trash2 size={15} />
                            </button>
                          </td>
                        </tr>
                      );
                    })}

                    {!positions.length && (
                      <tr>
                        <td className="wm-empty-cell" colSpan={6}>
                          Noch keine Positionen vorhanden.
                          Füge eine Position hinzu.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              <button
                className="wm-add-position-link"
                type="button"
                onClick={addPosition}
                disabled={!editable}
              >
                <Plus size={14} aria-hidden="true" />
                Position hinzufügen
              </button>
            </div>

            <aside className="wm-offer-preview-panel">
              <h2>
                <FileText size={17} aria-hidden="true" />
                Angebotsvorschau
              </h2>

              <div className="wm-offer-preview">
                {pdfLoading && <p role="status">PDF-Vorschau wird erstellt …</p>}
                {pdfError && <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{pdfError}</p>}
                {pdfUrl && (
                  <iframe
                    className="wm-offer-pdf-frame"
                    src={pdfUrl}
                    title={isDemo ? "Demo-Angebot als PDF" : "Angebot als PDF"}
                  />
                )}
                {!pdfLoading && !pdfError && !pdfUrl && (
                  <span>
                    Die PDF-Vorschau erscheint, sobald die KI die Angebotspositionen erstellt hat.
                  </span>
                )}
              </div>

              <div className="wm-offer-summary">
                <h3>Zusammenfassung</h3>
                {isDemo ? (
                  <p>Demo-Vorschau: fehlende Preise werden offengelassen; vorhandene KI-Preise sind unverbindliche Beispielwerte.</p>
                ) : (
                  <>
                    <p><span>Zwischensumme</span><strong>{money(total)}</strong></p>
                    <p><span>Material</span><strong>{money(total)}</strong></p>
                    <p><span>Arbeitsleistung</span><strong>{money(0)}</strong></p>
                    <p><span>MwSt. (19 %)</span><strong>{money(total * 0.19)}</strong></p>
                    <p className="wm-offer-grand-total"><span>Gesamtbetrag</span><strong>{money(total * 1.19)}</strong></p>
                  </>
                )}
              </div>

              {pdfUrl ? (
                <a
                  className="wm-preview-download"
                  href={pdfUrl}
                  download={isDemo ? "werkmundo-demo-angebot.pdf" : `angebot-${businessKey}.pdf`}
                >
                  <ArrowDownToLine size={14} aria-hidden="true" />
                  {isDemo ? "Demo-PDF herunterladen" : "PDF herunterladen"}
                </a>
              ) : (
                <button className="wm-preview-download" type="button" disabled>
                  <ArrowDownToLine size={14} aria-hidden="true" />
                  PDF wird vorbereitet …
                </button>
              )}
            </aside>
          </section>

          {isDemo && (
            <p className="wm-demo-disclaimer" role="status">
              Dieses Angebot ist eine Demo-Vorschau und nicht zur geschäftlichen Verwendung bestimmt.
            </p>
          )}
          {customerError && (
            <p className="wm-dashboard-feedback wm-dashboard-error" role="status">
              {customerError} Die Vorschau wird deshalb als Demo gekennzeichnet.
            </p>
          )}

          <div className="wm-offer-edit-actions">
            <Link
              className="wm-button wm-button-secondary"
              to="/dokumente?tab=angebote"
            >
              Abbrechen
            </Link>

            <button
              className="wm-button wm-button-secondary"
              type="button"
              onClick={() => void saveDraft()}
              disabled={!editable}
            >
              {saving ? "Wird gespeichert …" : "Entwurf speichern"}
            </button>

            <button
              className="wm-button wm-button-primary"
              type="button"
              onClick={() => void send()}
              disabled={busy || isDemo}
            >
              <Send size={15} aria-hidden="true" />
              {sending ? "Wird versendet …" : isDemo ? "Demo nicht versendbar" : "Angebot versenden"}
            </button>
          </div>

          <p className="wm-profile-hint">
            Änderungen werden mit „Entwurf speichern“ dauerhaft gespeichert.
          </p>
        </>
      )}
    </div>
  );
}