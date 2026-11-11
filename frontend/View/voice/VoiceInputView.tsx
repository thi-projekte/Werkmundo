import { ArrowRight, Mic, Pause, Plus, RotateCcw } from "lucide-react";
import "../dashboard/components/dashboard.css";
import "./components/HomeView.css";
import { DRAFT_CUSTOMER, useVoiceInputViewModel } from "../../ViewModel/voice/useVoiceInputViewModel";

const formatDuration = (seconds: number) =>
  `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;

/** Sprachaufnahme für ein neues Angebot. Reine Darstellung – Logik im useVoiceInputViewModel. */
export function VoiceInputView() {
  const vm = useVoiceInputViewModel();
  const busy = vm.state === "transcribing" || vm.state === "submitting";
  const showTranscript = vm.mode === "text" || vm.state === "transcribed" || vm.state === "submitting";

  return (
    <div className="dashboard-page wm-dashboard wm-recording-dashboard">
      <header className="wm-dashboard-heading">
        <div>
          <h1>
            Neues <span>Angebot</span>
          </h1>
        </div>
      </header>

      <section className="wm-offer-composer" aria-labelledby="wm-offer-title">
        <div className="wm-offer-intro">
          <div className="wm-offer-icon" aria-hidden="true">
            <Mic size={22} />
          </div>
          <div>
            <h2 id="wm-offer-title">Auftrag beschreiben</h2>
            <p>Sprich deinen Auftrag ein. Wir wandeln ihn in Text um und erstellen daraus einen Angebotsentwurf.</p>
          </div>
        </div>

        <div className="wm-customer-select-row">
          <label htmlFor="wm-customer-select">Wähle einen Kunden aus</label>
          <div className="wm-customer-select-controls">
            <select
              id="wm-customer-select"
              value={vm.selectedCustomerId}
              onChange={(event) => vm.selectCustomer(event.target.value)}
              aria-invalid={vm.fieldErrors.customer}
              disabled={busy}
            >
              <option value="">{vm.customersLoading ? "Kunden werden geladen …" : "Kunde auswählen"}</option>
              <option value={DRAFT_CUSTOMER}>Ohne Kunde als Entwurf speichern</option>
              {vm.customers.map((customer) => (
                <option key={customer.id} value={String(customer.id)}>
                  {[customer.firstName, customer.lastName].filter(Boolean).join(" ") || customer.email}
                </option>
              ))}
            </select>
            <button className="wm-add-customer" type="button" onClick={vm.goToCustomerSetup}>
              <Plus size={16} aria-hidden="true" /> Kunde anlegen
            </button>
          </div>
        </div>
        {vm.customersError && (
          <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{vm.customersError}</p>
        )}
        {vm.fieldErrors.customer && (
          <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
            Bitte wähle einen Kunden aus oder speichere ohne Kunden als Entwurf.
          </p>
        )}

        <div className="wm-offer-mode" role="group" aria-label="Eingabemethode">
          <button
            type="button"
            className={vm.mode === "voice" ? "is-active" : ""}
            aria-pressed={vm.mode === "voice"}
            onClick={() => vm.setMode("voice")}
          >
            Sprache
          </button>
          <button
            type="button"
            className={vm.mode === "text" ? "is-active" : ""}
            aria-pressed={vm.mode === "text"}
            onClick={() => vm.setMode("text")}
          >
            Text
          </button>
        </div>

        {vm.mode === "voice" && (vm.state === "idle" || vm.state === "recording") && (
          <div className={`wm-mic-stage ${vm.state === "recording" ? "is-recording" : ""}`}>
            <button
              className="wm-mic-trigger"
              type="button"
              onClick={vm.toggleRecording}
              aria-label={vm.state === "recording" ? "Aufnahme pausieren" : "Aufnahme starten"}
              style={{ transform: `scale(${1 + vm.volume / 250})` }}
            >
              {vm.state === "recording" ? (
                <Pause size={44} strokeWidth={1.6} aria-hidden="true" />
              ) : (
                <Mic size={48} strokeWidth={1.6} aria-hidden="true" />
              )}
            </button>
            <p aria-live="polite">
              {vm.state === "recording"
                ? `Ich höre zu … ${formatDuration(vm.elapsedSeconds)} / ${formatDuration(vm.maxSeconds)}`
                : "Tippe auf das Mikrofon, um deine Aufnahme zu starten."}
            </p>
            {vm.state === "recording" && (
              <button className="wm-stop-recording" type="button" onClick={vm.transcribe}>
                Aufnahme beenden
              </button>
            )}
          </div>
        )}

        {vm.mode === "voice" && (vm.state === "paused" || vm.state === "transcribing") && (
          <div className="wm-recording-review">
            <div className="wm-review-mic" aria-hidden="true">
              <Mic size={48} strokeWidth={1.6} />
            </div>
            <p aria-live="polite">
              {vm.state === "transcribing"
                ? "Deine Aufnahme wird in Text umgewandelt …"
                : `Aufnahme pausiert (${formatDuration(vm.elapsedSeconds)})`}
            </p>
            <div className="wm-audio-player">
              {vm.previewUrl ? (
                <audio controls src={vm.previewUrl} aria-label="Aufnahme abspielen" />
              ) : (
                <p>Keine Aufnahme vorhanden.</p>
              )}
            </div>
            <div className="wm-capture-actions">
              <button className="wm-capture-retry" type="button" onClick={vm.discardRecording} disabled={busy}>
                <RotateCcw size={16} aria-hidden="true" /> Neu aufnehmen
              </button>
              <button className="wm-capture-resume" type="button" onClick={vm.toggleRecording} disabled={busy}>
                <Mic size={16} aria-hidden="true" /> Aufnahme fortsetzen
              </button>
              <button className="wm-capture-next" type="button" onClick={vm.transcribe} disabled={busy}>
                {vm.state === "transcribing" ? "Wird verarbeitet …" : (
                  <>
                    <ArrowRight size={16} aria-hidden="true" /> Weiter
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {showTranscript && (
          <div className="wm-dashboard-text">
            {vm.mode === "voice" && vm.previewUrl && (
              <audio controls src={vm.previewUrl} aria-label="Aufnahme abspielen" />
            )}
            <input
              className="wm-draft-title"
              type="text"
              aria-label="Titel des Angebots"
              aria-invalid={vm.fieldErrors.title}
              placeholder={vm.selectedCustomerId === DRAFT_CUSTOMER ? "Titel Entwurf (Pflicht)" : "Titel (optional)"}
              maxLength={255}
              value={vm.title}
              onChange={(event) => vm.setTitle(event.target.value)}
              disabled={busy}
            />
            <textarea
              aria-label="Auftrag beschreiben"
              aria-invalid={vm.fieldErrors.transcript}
              placeholder="Gib hier deinen Text ein und die KI wird dir ein passendes Angebot erstellen."
              value={vm.transcript}
              onChange={(event) => vm.setTranscript(event.target.value)}
              disabled={busy}
            />
            {vm.fieldErrors.title && (
              <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
                Bitte gib für den Entwurf einen Titel ein.
              </p>
            )}
            {vm.fieldErrors.transcript && (
              <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
                Bitte beschreibe den Auftrag zuerst.
              </p>
            )}
            <div className="wm-capture-actions">
              {vm.mode === "voice" && (
                <button className="wm-capture-retry" type="button" onClick={vm.discardRecording} disabled={busy}>
                  <RotateCcw size={16} aria-hidden="true" /> Neu aufnehmen
                </button>
              )}
              <button className="wm-capture-next" type="button" onClick={vm.submit} disabled={busy}>
                {vm.state === "submitting" ? "Angebot wird erstellt …" : "Angebot erstellen"}
              </button>
            </div>
          </div>
        )}

        {vm.error && (
          <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">{vm.error}</p>
        )}
      </section>
    </div>
  );
}
