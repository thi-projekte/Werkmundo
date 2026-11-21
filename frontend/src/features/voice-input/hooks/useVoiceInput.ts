import { useCallback, useEffect, useRef, useState } from "react";
import { startMicrophone } from "../services/microphoneService";
import { getToken } from "@/services/authService";
import { API_CONFIG } from "@/config/api";

/** Maximale Aufnahmedauer: 20 Minuten. */
export const MAX_RECORDING_MS = 20 * 60 * 1000;

/** Ab dieser Restzeit wird eine Warnung angezeigt (1 Minute). */
export const WARNING_REMAINING_MS = 60 * 1000;

/**
 * Zustände der Sprachaufnahme (angelehnt an WhatsApp-Sprachnachrichten):
 * - idle:      noch nichts aufgenommen
 * - starting:  Mikrofon-Freigabe wird angefragt
 * - recording: Aufnahme läuft
 * - paused:    Aufnahme pausiert, kann fortgesetzt werden
 * - review:    Aufnahme beendet, kann angehört / gesendet / verworfen werden
 */
export type VoiceState = "idle" | "starting" | "recording" | "paused" | "review";

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

export const useVoiceInput = () => {
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

  const currentElapsed = () =>
    accumulatedMsRef.current +
    (segmentStartRef.current !== null ? Date.now() - segmentStartRef.current : 0);

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

  // ---------- Aufnahme beenden ----------

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
    setElapsedMs(accumulatedMsRef.current);
    stopTimer();
    stopVisualizer();

    const promise = new Promise<Blob>((resolve) => {
      recorder.onstop = () => {
        const type = (recorder.mimeType || "audio/webm").split(";")[0];
        const blob = new Blob(chunksRef.current, { type });
        blobRef.current = blob;
        setAudioUrl(URL.createObjectURL(blob));
        releaseResources();
        setState("review");
        stopPromiseRef.current = null;
        resolve(blob);
      };
    });
    stopPromiseRef.current = promise;
    recorder.stop();
    return promise;
  }, [releaseResources]);

  const startTimer = useCallback(() => {
    stopTimer();
    timerRef.current = window.setInterval(() => {
      const ms = currentElapsed();
      setElapsedMs(ms);
      // Nach 20 Minuten automatisch beenden
      if (ms >= MAX_RECORDING_MS) {
        void stop();
      }
    }, 200);
  }, [stop]);

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
      };

      audioContextRef.current = new AudioContext();
      startVisualizer();

      // timeslice: Daten regelmäßig sichern, damit bei langen Aufnahmen nichts verloren geht
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

  const pause = () => {
    const recorder = recorderRef.current;
    if (!recorder || recorder.state !== "recording") return;
    recorder.pause();
    accumulatedMsRef.current = currentElapsed();
    segmentStartRef.current = null;
    setElapsedMs(accumulatedMsRef.current);
    stopTimer();
    stopVisualizer();
    setState("paused");
  };

  const resume = () => {
    const recorder = recorderRef.current;
    if (!recorder || recorder.state !== "paused") return;
    if (accumulatedMsRef.current >= MAX_RECORDING_MS) return;
    recorder.resume();
    segmentStartRef.current = Date.now();
    startVisualizer();
    startTimer();
    setState("recording");
  };

  /** Aufnahme verwerfen (Mülleimer) und von vorn beginnen. */
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

  // Blob-URL freigeben, wenn sie ersetzt wird
  useEffect(() => {
    return () => {
      if (audioUrl) URL.revokeObjectURL(audioUrl);
    };
  }, [audioUrl]);

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
   * Beendet ggf. die laufende Aufnahme, schickt sie an
   * /speech-capture/transcribe und gibt das Transkript zurück.
   */
  const transcribeAudio = async (): Promise<string> => {
    const blob = await stop();
    if (blob.size === 0) {
      throw new Error("Die Aufnahme ist leer.");
    }

    const formData = new FormData();
    formData.append("audio", blob, `aufnahme.${fileExtensionFor(blob.type)}`);

    // Auth-Token mitschicken — der Endpunkt ist geschützt (sonst 401).
    // Content-Type NICHT setzen, damit der Browser die multipart-Boundary ergänzt.
    const token = await getToken();

    const response = await fetch(
      `${API_CONFIG.OFFER_SERVICE_URL}/speech-capture/transcribe`,
      {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      },
    );

    if (!response.ok) {
      const errorText = await response.text();
      if (response.status === 413) {
        throw new Error("Die Aufnahme ist zu groß für den Server.");
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
    maxMs: MAX_RECORDING_MS,
    remainingMs: Math.max(0, MAX_RECORDING_MS - elapsedMs),
    volume,
    error,
    audioUrl,
    transcript,
    setTranscript,
    start,
    pause,
    resume,
    stop,
    reset,
    transcribeAudio,
  };
};
