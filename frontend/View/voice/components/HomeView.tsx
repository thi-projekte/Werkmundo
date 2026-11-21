import { useState, useEffect } from "react";
import "./HomeView.css";
import { Mic } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  useVoiceInput,
  WARNING_REMAINING_MS,
} from "../../../ViewModel/voice/legacy/useVoiceInput";
import { createOffer, saveOfferDraft } from "../../../infrastructure/api/offerApi";
import { getCustomers, type CustomerProfile } from "../../../infrastructure/api/userApi";
import {
  ArrowRight,
  Pause,
  RotateCcw,
} from "lucide-react";

/** Formatiert Millisekunden als mm:ss. */
const formatTime = (ms: number) => {
  const totalSeconds = Math.floor(ms / 1000);
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
};

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
  const initialSaveAsDraft =
    initialRouteState &&
    typeof initialRouteState === "object" &&
    "saveAsDraft" in initialRouteState &&
    initialRouteState.saveAsDraft === true;

  // Kunden werden aus dem user-service geladen (GET /customers).
  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState<string | null>(null);

  const [mode, setMode] = useState<"voice" | "text">(initialMode);
  const [selectedCustomer, setSelectedCustomer] = useState<number | null>(
    initialCustomerId,
  );
  const [saveAsDraft, setSaveAsDraft] = useState(initialSaveAsDraft);
  const [draftTitle, setDraftTitle] = useState("");
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [transcribeError, setTranscribeError] = useState<string | null>(null);

  const {
    volume,
    toggle,
    resume,
    canResume,
    elapsedMs,
    maxMs,
    remainingMs,
    error: micError,
    audioUrl,
    transcript,
    setTranscript,
    state,
    reset,
    transcribeAudio,
  } = useVoiceInput();

  // Pausiert oder beendet -> Prüfansicht mit EINEM Player
  const isReview = state === "paused" || state === "finished";
  const reachedMax = elapsedMs >= maxMs;
  const showTimeWarning = state === "recording" && remainingMs <= WARNING_REMAINING_MS;

  const navigate = useNavigate();
  useEffect(() => {
    if (initialTranscript) setTranscript(initialTranscript);
  }, [initialTranscript, setTranscript]);
  const [customerError, setCustomerError] = useState(false);
  const [textError, setTextError] = useState(false);
  const [titleError, setTitleError] = useState(false);

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
      } finally {
        if (!cancelled) setCustomersLoading(false);
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
    customerId: number,
    speechSnippet: string,
  ) => {
    const offer = await createOffer({
      customerId: String(customerId),
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

  const speichereEntwurf = async (speechSnippet: string) => {
    await saveOfferDraft({
      title: draftTitle.trim(),
      speechSnippet,
    });
    navigate("/dashboard", {
      state: { notice: "Dein Entwurf wurde gespeichert." },
    });
  };

  const handleVoiceWeiter = async () => {
    const hasDestination = !!selectedCustomer || saveAsDraft;
    const hasTitle = !!draftTitle.trim();
    setCustomerError(!hasDestination);
    setTitleError(saveAsDraft && !hasTitle);
    if (!hasDestination || (saveAsDraft && !hasTitle)) return;

    setTranscribeError(null);
    setIsTranscribing(true);

    try {
      // transcribeAudio() beendet eine laufende/pausierte Aufnahme selbst
      // und schickt sie als EINE Datei.
      const speechSnippet = await transcribeAudio();
      if (saveAsDraft) {
        await speichereEntwurf(speechSnippet);
      } else if (selectedCustomer !== null) {
        await erstelleAngebotUndWeiter(selectedCustomer, speechSnippet);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unbekannter Fehler";
      setTranscribeError(`Angebot konnte nicht erstellt werden: ${message}`);
    } finally {
      setIsTranscribing(false);
    }
  };

  const handleTextWeiter = async () => {
    const hasDestination = !!selectedCustomer || saveAsDraft;
    const hasText = !!transcript.trim();
    const hasTitle = !!draftTitle.trim();
    setCustomerError(!hasDestination);
    setTextError(!hasText);
    setTitleError(saveAsDraft && !hasTitle);
    if (!hasDestination || !hasText || (saveAsDraft && !hasTitle)) return;

    setTranscribeError(null);
    setIsTranscribing(true);

    try {
      if (saveAsDraft) {
        await speichereEntwurf(transcript);
      } else if (selectedCustomer !== null) {
        await erstelleAngebotUndWeiter(selectedCustomer, transcript);
      }
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
              value={saveAsDraft ? "draft" : selectedCustomer ?? ""}
              onChange={(event) => {
                const value = event.target.value;
                setSaveAsDraft(value === "draft");
                setSelectedCustomer(value && value !== "draft" ? Number(value) : null);
                setCustomerError(false);
              }}
              aria-invalid={customerError}
            >
              <option value="">
                {customersLoading ? "Kunden werden geladen …" : "Kunde auswählen"}
              </option>
              <option value="draft">Ohne Kunde als Entwurf speichern</option>
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
        {customerError && (
          <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
            Bitte wähle einen Kunden aus oder speichere ohne Kunden als Entwurf.
          </p>
        )}
        {titleError && (
          <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
            Bitte gib für den Entwurf einen Titel ein.
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

        {mode === "voice" && !isReview && (
          <div className={`wm-mic-stage ${state === "recording" ? "is-recording" : ""}`}>
            <button
              className="wm-mic-trigger"
              type="button"
              onClick={toggle}
              disabled={state === "starting"}
              aria-label={state === "recording" ? "Aufnahme pausieren" : "Aufnahme starten"}
              title={state === "recording" ? "Aufnahme pausieren" : "Aufnahme starten"}
              style={{ transform: `scale(${1 + volume / 500})` }}
            >
              {/* Während der Aufnahme: Pausenzeichen "||" – signalisiert, dass man pausieren kann */}
              {state === "recording" ? (
                <Pause
                  className="wm-mic-pause-icon"
                  size={48}
                  strokeWidth={1.6}
                  fill="currentColor"
                  aria-hidden="true"
                />
              ) : (
                <Mic size={48} strokeWidth={1.6} aria-hidden="true" />
              )}
            </button>
            <p>
              {state === "recording"
                ? "Ich höre zu … Sprich jetzt deinen Auftrag. Tippe auf ‖, um zu pausieren."
                : state === "starting"
                  ? "Mikrofon wird gestartet …"
                  : "Tippe auf das Mikrofon, um deine Aufnahme zu starten (max. 20 Minuten)."}
            </p>
            {state === "recording" && (
              <>
                {/* Aufnahmeleiste wie bei WhatsApp */}
                <div className="wm-rec-bar" aria-live="off">
                  <span className="wm-rec-dot" aria-hidden="true" />
                  <span className="wm-rec-time">
                    {formatTime(elapsedMs)}
                    <small> / {formatTime(maxMs)}</small>
                  </span>
                  <div className="wm-rec-progress" aria-hidden="true">
                    <div
                      className="wm-rec-progress-fill"
                      style={{ width: `${Math.min(100, (elapsedMs / maxMs) * 100)}%` }}
                    />
                  </div>
                </div>
                {showTimeWarning && (
                  <p className="wm-rec-warning" role="status">
                    Noch {formatTime(remainingMs)} – die Aufnahme endet automatisch
                    nach 20 Minuten.
                  </p>
                )}
              </>
            )}
            {micError && (
              <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
                {micError}
              </p>
            )}
          </div>
        )}

        {mode === "voice" && isReview && (
          <div className="wm-recording-review">
            <input
              className="wm-draft-title"
              type="text"
              aria-label="Titel des Entwurfs"
              aria-invalid={titleError}
              placeholder="Titel Entwurf"
              maxLength={255}
              value={draftTitle}
              onChange={(event) => {
                setDraftTitle(event.target.value);
                if (event.target.value.trim()) setTitleError(false);
              }}
            />
            <div className="wm-review-mic" aria-hidden="true">
              <Mic size={48} strokeWidth={1.6} />
            </div>
            {/* EIN Player mit der kompletten bisherigen Aufnahme */}
            <div className="wm-audio-player">
              {audioUrl ? (
                <audio controls src={audioUrl} aria-label="Aufnahme abspielen" />
              ) : (
                <p>Aufnahme wird vorbereitet …</p>
              )}
            </div>
            <p className="wm-rec-summary">
              Aufnahme: {formatTime(elapsedMs)} / {formatTime(maxMs)}
              {reachedMax && " – maximale Dauer von 20 Minuten erreicht"}
            </p>
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
                onClick={resume}
                disabled={isTranscribing || !canResume}
                title={canResume ? "Weitersprechen – wird an dieselbe Aufnahme angehängt" : "Maximale Dauer erreicht"}
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
              aria-invalid={titleError}
              placeholder="Titel Entwurf"
              maxLength={255}
              value={draftTitle}
              onChange={(event) => {
                setDraftTitle(event.target.value);
                if (event.target.value.trim()) setTitleError(false);
              }}
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
