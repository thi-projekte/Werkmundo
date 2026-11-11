import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  MAX_RECORDING_SECONDS,
  type InputMode,
  type RecordingState,
  type VoiceInputRouteState,
} from "../../Model/voice/VoiceRecording";
import {
  describeMicrophoneError,
  startRecording,
  type MicrophoneSession,
} from "../../infrastructure/audio/microphoneService";
import { transcribeAudio } from "../../infrastructure/api/aiApi";
import { createOffer, saveOfferDraft } from "../../infrastructure/api/offerApi";
import { getCustomers, type CustomerProfile } from "../../infrastructure/api/userApi";

export const DRAFT_CUSTOMER = "draft";

const errorText = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

function readRouteState(state: unknown): VoiceInputRouteState {
  if (!state || typeof state !== "object") return {};
  const value = state as Record<string, unknown>;
  return {
    mode: value.mode === "text" ? "text" : value.mode === "voice" ? "voice" : undefined,
    customerId: typeof value.customerId === "string" ? value.customerId : undefined,
    transcript: typeof value.transcript === "string" ? value.transcript : undefined,
  };
}

/**
 * ViewModel der Sprachaufnahme: Aufnahme starten/pausieren/fortsetzen, an den ai-service zur
 * Transkription schicken, Transkript prüfen lassen und daraus ein Angebot anlegen.
 */
export function useVoiceInputViewModel() {
  const navigate = useNavigate();
  const initial = readRouteState(useLocation().state);

  const [mode, setModeState] = useState<InputMode>(initial.mode ?? "voice");
  const [state, setState] = useState<RecordingState>(initial.transcript ? "transcribed" : "idle");
  const [volume, setVolume] = useState(0);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [transcript, setTranscriptState] = useState(initial.transcript ?? "");
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({ customer: false, title: false, transcript: false });

  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState("");
  const [selectedCustomerId, setSelectedCustomerId] = useState(initial.customerId ?? "");

  const sessionRef = useRef<MicrophoneSession | null>(null);
  const levelFrameRef = useRef<number | undefined>(undefined);
  const timerRef = useRef<number | undefined>(undefined);

  // ---------- Kunden laden ----------
  useEffect(() => {
    let cancelled = false;
    getCustomers()
      .then((result) => !cancelled && setCustomers(result))
      .catch((loadError: unknown) => {
        if (!cancelled) setCustomersError(`Kunden konnten nicht geladen werden: ${errorText(loadError, "unbekannter Fehler")}`);
      })
      .finally(() => !cancelled && setCustomersLoading(false));
    return () => {
      cancelled = true;
    };
  }, []);

  // ---------- Hilfsfunktionen für Pegel und Timer ----------
  const stopMeters = useCallback(() => {
    if (levelFrameRef.current !== undefined) cancelAnimationFrame(levelFrameRef.current);
    if (timerRef.current !== undefined) window.clearInterval(timerRef.current);
    levelFrameRef.current = undefined;
    timerRef.current = undefined;
    setVolume(0);
  }, []);

  // Alte Vorschau-URLs gibt der Effekt weiter unten frei.
  const replacePreview = useCallback((blob: Blob | null) => {
    setPreviewUrl(blob && blob.size > 0 ? URL.createObjectURL(blob) : null);
  }, []);

  const pauseRecording = useCallback(async () => {
    const session = sessionRef.current;
    if (!session) return;
    stopMeters();
    replacePreview(await session.pause());
    setState("paused");
  }, [replacePreview, stopMeters]);

  const startMeters = useCallback(() => {
    const tick = () => {
      if (!sessionRef.current) return;
      setVolume(sessionRef.current.readLevel());
      levelFrameRef.current = requestAnimationFrame(tick);
    };
    tick();
    timerRef.current = window.setInterval(() => setElapsedSeconds((seconds) => seconds + 1), 1000);
  }, []);

  // Maximale Aufnahmedauer: automatisch pausieren.
  useEffect(() => {
    if (state === "recording" && elapsedSeconds >= MAX_RECORDING_SECONDS) void pauseRecording();
  }, [elapsedSeconds, pauseRecording, state]);

  // Beim Verlassen der Seite Mikrofon freigeben.
  useEffect(
    () => () => {
      stopMeters();
      sessionRef.current?.cancel();
      sessionRef.current = null;
    },
    [stopMeters],
  );
  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  // ---------- Aktionen für die View ----------
  const toggleRecording = async () => {
    setError("");
    if (state === "recording") {
      await pauseRecording();
      return;
    }
    if (state === "paused" && sessionRef.current) {
      sessionRef.current.resume();
      setState("recording");
      startMeters();
      return;
    }
    if (state !== "idle") return;
    try {
      sessionRef.current = await startRecording();
      setElapsedSeconds(0);
      setState("recording");
      startMeters();
    } catch (micError) {
      sessionRef.current = null;
      setError(describeMicrophoneError(micError));
    }
  };

  const discardRecording = () => {
    stopMeters();
    sessionRef.current?.cancel();
    sessionRef.current = null;
    replacePreview(null);
    setElapsedSeconds(0);
    setTranscriptState("");
    setError("");
    setState("idle");
  };

  /** Beendet die Aufnahme und lässt sie vom ai-service in Text umwandeln. */
  const transcribe = async () => {
    const session = sessionRef.current;
    if (!session) return;
    stopMeters();
    setError("");
    setState("transcribing");
    try {
      const audio = await session.stop();
      sessionRef.current = null;
      replacePreview(audio);
      if (audio.size === 0) throw new Error("Die Aufnahme ist leer. Bitte erneut aufnehmen.");
      setTranscriptState(await transcribeAudio(audio));
      setState("transcribed");
    } catch (transcribeError) {
      setError(`Transkription fehlgeschlagen: ${errorText(transcribeError, "unbekannter Fehler")}`);
      // Aufnahme ist beendet – Nutzer kann neu aufnehmen oder auf Text wechseln.
      setState("idle");
    }
  };

  const setTranscript = (value: string) => {
    setTranscriptState(value);
    if (value.trim()) setFieldErrors((current) => ({ ...current, transcript: false }));
  };

  const selectCustomer = (customerId: string) => {
    setSelectedCustomerId(customerId);
    setFieldErrors((current) => ({ ...current, customer: false }));
  };

  const updateTitle = (value: string) => {
    setTitle(value);
    if (value.trim()) setFieldErrors((current) => ({ ...current, title: false }));
  };

  const setMode = (nextMode: InputMode) => {
    if (state === "recording" || state === "transcribing" || state === "submitting") return;
    setModeState(nextMode);
    setError("");
  };

  /** Übergibt das Transkript an den offer-service (startet dort den Prozess inkl. KI-Analyse). */
  const submit = async () => {
    const isDraft = selectedCustomerId === DRAFT_CUSTOMER;
    const errors = {
      customer: !selectedCustomerId,
      title: isDraft && !title.trim(),
      transcript: !transcript.trim(),
    };
    setFieldErrors(errors);
    if (errors.customer || errors.title || errors.transcript) return;

    setError("");
    const previousState = state;
    setState("submitting");
    try {
      if (isDraft) {
        await saveOfferDraft({ title: title.trim(), speechSnippet: transcript.trim() });
        navigate("/dashboard", { state: { notice: "Dein Entwurf wurde gespeichert." } });
      } else {
        await createOffer({
          customerId: selectedCustomerId,
          speechSnippet: transcript.trim(),
          ...(title.trim() ? { title: title.trim() } : {}),
        });
        navigate("/dashboard", {
          state: { notice: "Dein Angebot wird erstellt. Die KI bereitet gerade die Positionen vor." },
        });
      }
    } catch (submitError) {
      setError(`Angebot konnte nicht erstellt werden: ${errorText(submitError, "unbekannter Fehler")}`);
      setState(previousState);
    }
  };

  return {
    mode,
    setMode,
    state,
    volume,
    elapsedSeconds,
    maxSeconds: MAX_RECORDING_SECONDS,
    previewUrl,
    transcript,
    setTranscript,
    title,
    setTitle: updateTitle,
    error,
    fieldErrors,
    customers,
    customersLoading,
    customersError,
    selectedCustomerId,
    selectCustomer,
    toggleRecording,
    discardRecording,
    transcribe,
    submit,
    goToCustomerSetup: () => navigate("/einrichtung/unternehmen"),
  };
}
