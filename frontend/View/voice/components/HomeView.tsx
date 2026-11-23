import { useState, useEffect } from "react";
import "./HomeView.css";
import { Mic } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { useVoiceInput } from "../../../ViewModel/voice/legacy/useVoiceInput";
import { createOffer } from "../../../infrastructure/api/offerApi";
import { getCustomers, type CustomerProfile } from "../../../infrastructure/api/userApi";
import {
  ArrowRight,
  RotateCcw,
} from "lucide-react";

export const HomeView = () => {
  const location = useLocation();
  const initialRouteState = location.state;
  const initialMode =
    initialRouteState &&
    typeof initialRouteState === "object" &&
    "mode" in initialRouteState &&
    initialRouteState.mode === "text"
      ? "text"
      : "voice";
  const initialTranscript =
    initialRouteState &&
    typeof initialRouteState === "object" &&
    "transcript" in initialRouteState &&
    typeof initialRouteState.transcript === "string"
      ? initialRouteState.transcript
      : "";
  const initialCustomerId =
    initialRouteState &&
    typeof initialRouteState === "object" &&
    "customerId" in initialRouteState &&
    typeof initialRouteState.customerId === "number"
      ? initialRouteState.customerId
      : null;
  // Kunden werden aus dem user-service geladen (GET /customers).
  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [customersError, setCustomersError] = useState<string | null>(null);

  const [mode, setMode] = useState<"voice" | "text">(initialMode);
  const [selectedCustomer, setSelectedCustomer] = useState<number | null>(
    initialCustomerId,
  );
  const [draftTitle, setDraftTitle] = useState("");
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [transcribeError, setTranscribeError] = useState<string | null>(null);

  const {
    volume,
    toggle,
    audioSegments,
    transcript,
    setTranscript,
    state,
    reset,
    finalizeRecording,
    transcribeAudio,
  } = useVoiceInput();

  const navigate = useNavigate();
  useEffect(() => {
    if (initialTranscript) setTranscript(initialTranscript);
  }, [initialTranscript, setTranscript]);
  const [textError, setTextError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getCustomers();
        if (!cancelled) setCustomers(data);
      } catch (err) {
        if (!cancelled) {
          const message =
            err instanceof Error ? err.message : "Unbekannter Fehler";
          setCustomersError(`Kunden konnten nicht geladen werden: ${message}`);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  /**
   * Erstellt das Angebot im offer-service (startet damit den PE-/KI-Flow) und
   * navigiert anschließend zur Laden-Seite, die per Polling auf das KI-Ergebnis
   * wartet und dann den businessKey an /review weiterreicht.
   */
  const erstelleAngebotUndWeiter = async (
    customerId: number | null,
    speechSnippet: string,
  ) => {
    const offer = await createOffer({
      customerId: customerId === null ? null : String(customerId),
      speechSnippet,
      ...(draftTitle.trim() ? { title: draftTitle.trim() } : {}),
    });
    navigate(`/angebote/${encodeURIComponent(offer.businessKey)}/bearbeiten`, {
      state: {
        businessKey: offer.businessKey,
        offerId: offer.id,
      },
    });
  };

  const handleVoiceWeiter = async () => {
    finalizeRecording();
    setTranscribeError(null);
    setIsTranscribing(true);

    try {
      const speechSnippet = await transcribeAudio();
      await erstelleAngebotUndWeiter(selectedCustomer, speechSnippet);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unbekannter Fehler";
      setTranscribeError(`Angebot konnte nicht erstellt werden: ${message}`);
    } finally {
      setIsTranscribing(false);
    }
  };

  const handleTextWeiter = async () => {
    const hasText = !!transcript.trim();
    setTextError(!hasText);
    if (!hasText) return;

    setTranscribeError(null);
    setIsTranscribing(true);

    try {
      await erstelleAngebotUndWeiter(selectedCustomer, transcript);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unbekannter Fehler";
      setTranscribeError(`Angebot konnte nicht erstellt werden: ${message}`);
    } finally {
      setIsTranscribing(false);
    }
  };

  return (
    <div className="dashboard-page wm-dashboard wm-recording-dashboard">
      <header className="wm-dashboard-heading">
        <div>
          <h1>Willkommen <span>zurück!</span></h1>
        </div>
      </header>

      <section className="wm-offer-composer" aria-labelledby="wm-offer-title">
        <div className="wm-offer-intro">
          <div className="wm-offer-icon" aria-hidden="true">
            <Mic size={22} />
          </div>
          <div>
            <h2 id="wm-offer-title">Neues Angebot erstellen</h2>
            <p>Beschreibe deinen Auftrag. Wir erstellen dir daraus einen Angebotsentwurf.</p>
          </div>
        </div>

        <div className="wm-customer-select-row">
          <label htmlFor="wm-customer-select">Wähle einen Kunden aus</label>
          <div className="wm-customer-select-controls">
            <select
              id="wm-customer-select"
              value={selectedCustomer ?? ""}
              onChange={(event) => {
                const value = event.target.value;
                setSelectedCustomer(value ? Number(value) : null);
              }}
            >
              <option value="">Ohne Kunden – Demo-Vorschau</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {[customer.firstName, customer.lastName].filter(Boolean).join(" ").trim() ||
                    customer.email}
                  {customer.companyName ? ` · ${customer.companyName}` : ""}
                </option>
              ))}
            </select>
            <button
              className="wm-add-customer"
              type="button"
              onClick={() => navigate("/kunden/neu")}
            >
              + Kunde anlegen
            </button>
          </div>
        </div>
        {customersError && (
          <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
            {customersError}
          </p>
        )}
        <div className="wm-offer-mode" role="group" aria-label="Eingabemethode">
          <button
            type="button"
            className={mode === "voice" ? "is-active" : ""}
            aria-pressed={mode === "voice"}
            onClick={() => setMode("voice")}
          >
            Sprache
          </button>
          <button
            type="button"
            className={mode === "text" ? "is-active" : ""}
            aria-pressed={mode === "text"}
            onClick={() => setMode("text")}
          >
            Text
          </button>
        </div>

        {mode === "voice" && state !== "review" && (
          <div className={`wm-mic-stage ${state === "recording" ? "is-recording" : ""}`}>
            <button
              className="wm-mic-trigger"
              type="button"
              onClick={toggle}
              aria-label={state === "recording" ? "Aufnahme beenden" : "Aufnahme starten"}
              style={{ transform: `scale(${1 + volume / 500})` }}
            >
              <Mic size={48} strokeWidth={1.6} aria-hidden="true" />
            </button>
            <p>
              {state === "recording"
                ? "Ich höre zu … Sprich jetzt deinen Auftrag."
                : "Tippe auf das Mikrofon, um deine Aufnahme zu starten."}
            </p>
            {state === "recording" && (
              <button className="wm-stop-recording" type="button" onClick={toggle}>
                Aufnahme beenden
              </button>
            )}
          </div>
        )}

        {mode === "voice" && state === "review" && (
          <div className="wm-recording-review">
            <input
              className="wm-draft-title"
              type="text"
              aria-label="Titel des Entwurfs"
              placeholder="Titel Entwurf"
              maxLength={255}
              value={draftTitle}
              onChange={(event) => setDraftTitle(event.target.value)}
            />
            <div className="wm-review-mic" aria-hidden="true">
              <Mic size={48} strokeWidth={1.6} />
            </div>
            <div className="wm-audio-player">
              {audioSegments.length > 0 ? (
                audioSegments.map((url, index) => (
                  <audio
                    controls
                    src={url}
                    key={`${url}-${index}`}
                    aria-label={`Aufnahme ${index + 1} abspielen`}
                  />
                ))
              ) : (
                <p>Keine Aufnahme vorhanden.</p>
              )}
            </div>
            {transcribeError && (
              <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
                {transcribeError}
              </p>
            )}
            <div className="wm-capture-actions">
              <button
                className="wm-capture-retry"
                type="button"
                onClick={reset}
                disabled={isTranscribing}
              >
                <RotateCcw size={16} aria-hidden="true" />
                Neu aufnehmen
              </button>
              <button
                className="wm-capture-resume"
                type="button"
                onClick={toggle}
                disabled={isTranscribing}
              >
                <Mic size={16} aria-hidden="true" />
                Aufnahme fortsetzen
              </button>
              <button
                className="wm-capture-next"
                type="button"
                onClick={handleVoiceWeiter}
                disabled={isTranscribing}
              >
                {isTranscribing ? (
                  "Wird verarbeitet …"
                ) : (
                  <>
                    <ArrowRight size={16} aria-hidden="true" />
                    Weiter
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {mode === "text" && (
          <div className="wm-dashboard-text">
            <input
              className="wm-draft-title"
              type="text"
              aria-label="Titel des Entwurfs"
              placeholder="Titel Entwurf"
              maxLength={255}
              value={draftTitle}
              onChange={(event) => setDraftTitle(event.target.value)}
            />
            <textarea
              aria-label="Auftrag beschreiben"
              aria-invalid={textError}
              placeholder="Gib hier deinen Text ein und die KI wird dir ein passendes Angebot erstellen."
              value={transcript}
              onChange={(event) => {
                setTranscript(event.target.value);
                if (event.target.value.trim()) setTextError(false);
              }}
            />
            {transcribeError && (
              <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
                {transcribeError}
              </p>
            )}
            {textError && (
              <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
                Bitte beschreibe den Auftrag zuerst.
              </p>
            )}
            <button
              type="button"
              disabled={isTranscribing}
              onClick={handleTextWeiter}
            >
              {isTranscribing ? "Angebot wird erstellt …" : "Angebot erstellen"}
            </button>
          </div>
        )}
      </section>
    </div>
  );
};
