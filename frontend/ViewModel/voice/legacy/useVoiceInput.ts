import { useCallback, useEffect, useRef, useState } from "react";
import { startMicrophone } from "../../../infrastructure/audio/microphoneService";
import { authorizedFetch } from "../../../infrastructure/http/apiClient";

const OFFER_SERVICE_URL =
  import.meta.env.VITE_OFFER_SERVICE_URL || "http://localhost:8082";

/** Maximale Aufnahmedauer: 20 Minuten. */
export const MAX_RECORDING_MS = 20 * 60 * 1000;

/** Ab dieser Restzeit wird eine Warnung angezeigt (1 Minute). */
export const WARNING_REMAINING_MS = 60 * 1000;

/**
 * Zustände der Sprachaufnahme (wie bei WhatsApp-Sprachnachrichten):
 * - idle:      noch nichts aufgenommen
 * - starting:  Mikrofon-Freigabe wird angefragt
 * - recording: Aufnahme läuft
 * - paused:    pausiert – bisherige Aufnahme anhörbar, kann fortgesetzt werden
 * - finished:  endgültig beendet (z. B. 20 Minuten erreicht), nur noch senden/verwerfen
 *
 * WICHTIG: Es gibt während der gesamten Aufnahme nur EINEN MediaRecorder.
 * Pause/Fortsetzen nutzt recorder.pause()/resume(). Dadurch entsteht immer
 * genau EINE gültige Audiodatei statt mehrerer Einzelsegmente.
 */
export type VoiceState = "idle" | "starting" | "recording" | "paused" | "finished";

/** Bevorzugte Formate – Safari/iOS kann kein WebM, nimmt dann MP4 auf. */
const PREFERRED_MIME_TYPES = [
  "audio/webm;codecs=opus",
  "audio/webm",
  "audio/mp4",
  "audio/ogg;codecs=opus",
];

const pickMimeType = (): string | undefined => {
  if (typeof MediaRecorder === "undefined") return undefined;
  return PREFERRED_MIME_TYPES.find((t) => MediaRecorder.isTypeSupported(t));
};

const fileExtensionFor = (mimeType: string) => {
  if (mimeType.includes("mp4")) return "m4a";
  if (mimeType.includes("ogg")) return "ogg";
  return "webm";
};

/** Übersetzt Browser-Fehler beim Mikrofonzugriff in verständliche Meldungen. */
const microphoneErrorMessage = (err: unknown): string => {
  if (!window.isSecureContext || !navigator.mediaDevices) {
    return "Mikrofon ist nur über HTTPS oder localhost verfügbar.";
  }
  if (typeof MediaRecorder === "undefined") {
    return "Dieser Browser unterstützt keine Sprachaufnahme.";
  }
  const name = err instanceof DOMException ? err.name : "";
  switch (name) {
    case "NotAllowedError":
    case "SecurityError":
      return "Mikrofonzugriff wurde verweigert. Bitte in den Browser-Einstellungen erlauben.";
    case "NotFoundError":
      return "Kein Mikrofon gefunden.";
    case "NotReadableError":
      return "Das Mikrofon wird gerade von einer anderen Anwendung verwendet.";
    default:
      return `Mikrofon konnte nicht gestartet werden${err instanceof Error ? `: ${err.message}` : "."}`;
  }
};

export const useVoiceInput = (
  { maxMs = MAX_RECORDING_MS }: { maxMs?: number } = {},
) => {
  const [state, setState] = useState<VoiceState>("idle");
  const [elapsedMs, setElapsedMs] = useState(0);
  const [volume, setVolume] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [transcript, setTranscript] = useState("");

  const streamRef = useRef<MediaStream | null>(null);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const blobRef = useRef<Blob | null>(null);
  const stopPromiseRef = useRef<Promise<Blob> | null>(null);

  const audioContextRef = useRef<AudioContext | null>(null);
  const animationRef = useRef<number | undefined>(undefined);

  // Zeitmessung: bereits aufgenommene Zeit + Startzeitpunkt des laufenden Abschnitts
  const accumulatedMsRef = useRef(0);
  const segmentStartRef = useRef<number | null>(null);
  const timerRef = useRef<number | undefined>(undefined);
  const limitTimeoutRef = useRef<number | undefined>(undefined);
  // Immer aktuelle enforceLimit-Funktion für Event-Handler (Recorder, Sichtbarkeit)
  const enforceLimitRef = useRef<() => void>(() => {});

  const currentElapsed = () =>
    accumulatedMsRef.current +
    (segmentStartRef.current !== null ? Date.now() - segmentStartRef.current : 0);

  const mimeTypeOf = (recorder: MediaRecorder) =>
    (recorder.mimeType || "audio/webm").split(";")[0];

  /** Erzeugt aus ALLEN bisherigen Chunks eine abspielbare Datei. */
  const buildBlob = (recorder: MediaRecorder) =>
    new Blob(chunksRef.current, { type: mimeTypeOf(recorder) });

  const replaceAudioUrl = (blob: Blob) => {
    setAudioUrl((old) => {
      if (old) URL.revokeObjectURL(old);
      return URL.createObjectURL(blob);
    });
  };

  // ---------- Visualisierung ----------

  const stopVisualizer = () => {
    if (animationRef.current) cancelAnimationFrame(animationRef.current);
    animationRef.current = undefined;
    setVolume(0);
  };

  const startVisualizer = () => {
    const ctx = audioContextRef.current;
    const stream = streamRef.current;
    if (!ctx || !stream) return;

    const analyser = ctx.createAnalyser();
    analyser.fftSize = 256;
    ctx.createMediaStreamSource(stream).connect(analyser);
    const data = new Uint8Array(analyser.frequencyBinCount);

    let last = -1;
    const update = () => {
      analyser.getByteFrequencyData(data);
      const avg = data.reduce((a, b) => a + b, 0) / data.length;
      // Nur bei spürbarer Änderung neu rendern (statt 60x pro Sekunde)
      const rounded = Math.round(avg / 4) * 4;
      if (rounded !== last) {
        last = rounded;
        setVolume(rounded);
      }
      animationRef.current = requestAnimationFrame(update);
    };
    update();
  };

  // ---------- Timer ----------

  const stopTimer = () => {
    if (timerRef.current) window.clearInterval(timerRef.current);
    timerRef.current = undefined;
    if (limitTimeoutRef.current) window.clearTimeout(limitTimeoutRef.current);
    limitTimeoutRef.current = undefined;
  };

  // ---------- Ressourcen freigeben ----------

  /** Mikrofon, AudioContext, Timer und Animation freigeben. */
  const releaseResources = useCallback(() => {
    stopTimer();
    if (animationRef.current) cancelAnimationFrame(animationRef.current);
    animationRef.current = undefined;
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
    if (audioContextRef.current && audioContextRef.current.state !== "closed") {
      void audioContextRef.current.close();
    }
    audioContextRef.current = null;
  }, []);

  // ---------- Aufnahme endgültig beenden ----------

  /**
   * Beendet die Aufnahme endgültig und liefert die komplette Aufnahme als
   * EINE gültige Audiodatei. Mehrfaches Aufrufen ist sicher.
   */
  const stop = useCallback((): Promise<Blob> => {
    if (stopPromiseRef.current) return stopPromiseRef.current;
    const recorder = recorderRef.current;

    if (!recorder || recorder.state === "inactive") {
      return blobRef.current
        ? Promise.resolve(blobRef.current)
        : Promise.reject(new Error("Keine Audioaufnahme vorhanden."));
    }

    accumulatedMsRef.current = currentElapsed();
    segmentStartRef.current = null;
    setElapsedMs(Math.min(accumulatedMsRef.current, maxMs));
    stopTimer();
    stopVisualizer();

    const promise = new Promise<Blob>((resolve) => {
      recorder.onstop = () => {
        const blob = buildBlob(recorder);
        blobRef.current = blob;
        replaceAudioUrl(blob);
        releaseResources();
        setState("finished");
        stopPromiseRef.current = null;
        resolve(blob);
      };
    });
    stopPromiseRef.current = promise;
    recorder.stop();
    return promise;
  }, [releaseResources, maxMs]);

  /**
   * Prüft das Gesamtlimit (Summe aller Abschnitte, Pausen zählen nicht) und
   * beendet die Aufnahme bei 20:00. Wird an mehreren Stellen aufgerufen, weil
   * Browser Timer in Hintergrund-Tabs stark drosseln (Chrome: 1x pro Minute).
   */
  const enforceLimit = useCallback(() => {
    const recorder = recorderRef.current;
    if (!recorder || recorder.state !== "recording") return;
    if (currentElapsed() >= maxMs) {
      void stop();
    }
  }, [stop, maxMs]);

  const startTimer = useCallback(() => {
    stopTimer();
    // 1) Anzeige + Prüfung (kann im Hintergrund gedrosselt werden)
    timerRef.current = window.setInterval(() => {
      setElapsedMs(Math.min(currentElapsed(), maxMs));
      enforceLimit();
    }, 200);
    // 2) Exakter Timeout auf die Restzeit
    const remaining = Math.max(0, maxMs - currentElapsed());
    limitTimeoutRef.current = window.setTimeout(enforceLimit, remaining);
  }, [enforceLimit, maxMs]);

  // ---------- Aufnahme starten / pausieren / fortsetzen ----------

  const start = async () => {
    if (state !== "idle") return; // schützt vor Doppelklick
    setError(null);
    setState("starting");

    try {
      if (!navigator.mediaDevices || typeof MediaRecorder === "undefined") {
        throw new Error("unsupported");
      }
      const stream = await startMicrophone();
      streamRef.current = stream;

      const mimeType = pickMimeType();
      const recorder = new MediaRecorder(stream, {
        ...(mimeType ? { mimeType } : {}),
        audioBitsPerSecond: 32000, // Sprache: 20 Min. ≈ 5 MB
      });
      recorderRef.current = recorder;
      chunksRef.current = [];
      blobRef.current = null;

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data);
        // 3) Kommt jede Sekunde vom Audio-System – wird im Hintergrund NICHT gedrosselt
        enforceLimitRef.current();
      };

      audioContextRef.current = new AudioContext();
      startVisualizer();

      // timeslice: Daten jede Sekunde sichern, damit bei langen Aufnahmen nichts verloren geht
      recorder.start(1000);
      accumulatedMsRef.current = 0;
      segmentStartRef.current = Date.now();
      setElapsedMs(0);
      startTimer();
      setState("recording");
    } catch (err) {
      releaseResources();
      setError(microphoneErrorMessage(err));
      setState("idle");
    }
  };

  /** Pausiert die Aufnahme. Die bisherige Aufnahme kann angehört werden. */
  const pause = () => {
    const recorder = recorderRef.current;
    if (!recorder || recorder.state !== "recording") return;

    recorder.pause();
    accumulatedMsRef.current = currentElapsed();
    segmentStartRef.current = null;
    setElapsedMs(Math.min(accumulatedMsRef.current, maxMs));
    stopTimer();
    stopVisualizer();
    setState("paused");

    // Restliche Daten anfordern und daraus EINE Vorschau-Datei bauen
    // (alle Chunks desselben Recorders ergeben eine gültige Datei).
    recorder.addEventListener(
      "dataavailable",
      () => replaceAudioUrl(buildBlob(recorder)),
      { once: true },
    );
    recorder.requestData();
  };

  /** Setzt dieselbe Aufnahme fort – das Gesprochene wird hinten angehängt. */
  const resume = () => {
    const recorder = recorderRef.current;
    if (!recorder || recorder.state !== "paused") return;
    if (accumulatedMsRef.current >= maxMs) return;

    recorder.resume();
    segmentStartRef.current = Date.now();
    if (audioContextRef.current?.state === "suspended") {
      void audioContextRef.current.resume();
    }
    startVisualizer();
    startTimer();
    setState("recording");
  };

  /** Ein Button für alles: Start → Pause → Fortsetzen. */
  const toggle = () => {
    if (state === "idle") void start();
    else if (state === "recording") pause();
    else if (state === "paused") resume();
  };

  /** Aufnahme verwerfen und von vorn beginnen. */
  const reset = () => {
    const recorder = recorderRef.current;
    if (recorder && recorder.state !== "inactive") {
      recorder.onstop = null;
      recorder.stop();
    }
    releaseResources();
    stopPromiseRef.current = null;
    recorderRef.current = null;
    chunksRef.current = [];
    blobRef.current = null;
    accumulatedMsRef.current = 0;
    segmentStartRef.current = null;
    setAudioUrl((old) => {
      if (old) URL.revokeObjectURL(old);
      return null;
    });
    setElapsedMs(0);
    setVolume(0);
    setTranscript("");
    setError(null);
    setState("idle");
  };

  enforceLimitRef.current = enforceLimit;

  // 4) Beim Zurückkehren in den Tab sofort prüfen und Anzeige aktualisieren
  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState !== "visible") return;
      if (recorderRef.current?.state === "recording") {
        setElapsedMs(Math.min(currentElapsed(), maxMs));
      }
      enforceLimitRef.current();
    };
    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, [maxMs]);

  // Beim Verlassen der Seite Mikrofon freigeben
  useEffect(() => {
    return () => {
      const recorder = recorderRef.current;
      if (recorder && recorder.state !== "inactive") {
        recorder.onstop = null;
        recorder.stop();
      }
      releaseResources();
    };
  }, [releaseResources]);

  // ---------- Transkription ----------

  /**
   * Beendet ggf. die laufende/pausierte Aufnahme, schickt sie als EINE Datei an
   * /speech-capture/transcribe und gibt das Transkript zurück.
   */
  const transcribeAudio = async (): Promise<string> => {
    const blob = await stop();
    if (blob.size === 0) {
      throw new Error("Die Aufnahme ist leer.");
    }

    const formData = new FormData();
    formData.append("audio", blob, `aufnahme.${fileExtensionFor(blob.type)}`);

    const response = await authorizedFetch(
      `${OFFER_SERVICE_URL}/speech-capture/transcribe`,
      {
        method: "POST",
        body: formData,
      },
    );

    if (!response.ok) {
      const errorText = await response.text();
      if (response.status === 401) {
        throw new Error("Nicht angemeldet (401). Bitte erneut einloggen.");
      }
      if (response.status === 403) {
        throw new Error(
          "Keine Berechtigung (403). Für die Spracheingabe braucht dein Benutzer die Rolle OWNER.",
        );
      }
      if (response.status === 413) {
        throw new Error("Die Aufnahme ist zu groß für den Server (413).");
      }
      throw new Error(
        `Transkription fehlgeschlagen (${response.status}): ${errorText}`,
      );
    }

    const data = await response.json();
    return data.transkript as string;
  };

  return {
    state,
    isRecording: state === "recording",
    elapsedMs,
    maxMs: maxMs,
    remainingMs: Math.max(0, maxMs - elapsedMs),
    canResume: state === "paused" && elapsedMs < maxMs,
    volume,
    error,
    audioUrl,
    transcript,
    setTranscript,
    start,
    pause,
    resume,
    toggle,
    stop,
    reset,
    transcribeAudio,
  };
};
