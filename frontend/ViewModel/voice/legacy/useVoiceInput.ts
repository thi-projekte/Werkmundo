import { useRef, useState } from "react";
import { startMicrophone } from "../../../infrastructure/audio/microphoneService";
import { authorizedFetch } from "../../../infrastructure/http/apiClient";

const OFFER_SERVICE_URL =
  import.meta.env.VITE_OFFER_SERVICE_URL || "http://localhost:8082";

/** Maximale Gesamtlänge der (einen) Tonspur: 20 Minuten. Pausen zählen nicht. */
export const MAX_RECORDING_MS = 20 * 60 * 1000;

export const useVoiceInput = () => {
  const [isRecording, setIsRecording] = useState(false);
  const [volume, setVolume] = useState(0);
  const [transcript, setTranscript] = useState("");
  // Es gibt genau EINE Tonspur – neue Abschnitte werden hinten angehängt.
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [elapsedMs, setElapsedMs] = useState(0);
  const [state, setState] = useState<
    "idle" | "recording" | "review" | "finished"
  >("idle");

  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const animationRef = useRef<number | undefined>(undefined);

  const streamRef = useRef<MediaStream | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  // Zeitmessung: bereits aufgenommene Zeit + Startzeitpunkt des laufenden Abschnitts
  const recordedMsRef = useRef(0);
  const segmentStartRef = useRef<number | null>(null);
  const timerRef = useRef<number | undefined>(undefined);

  const limitReached = elapsedMs >= MAX_RECORDING_MS;

  const currentElapsed = () =>
    recordedMsRef.current +
    (segmentStartRef.current ? Date.now() - segmentStartRef.current : 0);

  const buildBlob = () =>
    new Blob(chunksRef.current, {
      type: mediaRecorderRef.current?.mimeType || "audio/webm",
    });

  const updateAudioUrl = () => {
    const url = URL.createObjectURL(buildBlob());
    setAudioUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return url;
    });
  };

  const startVisualisation = () => {
    const analyser = analyserRef.current;
    if (!analyser) return;
    const dataArray = new Uint8Array(analyser.frequencyBinCount);
    const update = () => {
      analyser.getByteFrequencyData(dataArray);
      const avg = dataArray.reduce((a, b) => a + b, 0) / dataArray.length;
      setVolume(avg);
      animationRef.current = requestAnimationFrame(update);
    };
    update();
  };

  const stopVisualisation = () => {
    if (animationRef.current) cancelAnimationFrame(animationRef.current);
    animationRef.current = undefined;
    setVolume(0);
  };

  const startTimer = () => {
    segmentStartRef.current = Date.now();
    window.clearInterval(timerRef.current);
    timerRef.current = window.setInterval(() => {
      const elapsed = currentElapsed();
      setElapsedMs(Math.min(elapsed, MAX_RECORDING_MS));
      // 20-Minuten-Limit erreicht → Aufnahme automatisch pausieren
      if (elapsed >= MAX_RECORDING_MS) pause();
    }, 250);
  };

  const stopTimer = () => {
    window.clearInterval(timerRef.current);
    recordedMsRef.current = Math.min(currentElapsed(), MAX_RECORDING_MS);
    segmentStartRef.current = null;
    setElapsedMs(recordedMsRef.current);
  };

  const start = async () => {
    setState("recording");
    const stream = await startMicrophone();
    streamRef.current = stream;

    // Audio-Visualisierung (bleibt über Pausen hinweg bestehen)
    const audioContext = new AudioContext();
    const analyser = audioContext.createAnalyser();
    audioContext.createMediaStreamSource(stream).connect(analyser);
    analyser.fftSize = 256;
    audioContextRef.current = audioContext;
    analyserRef.current = analyser;
    startVisualisation();

    // EIN MediaRecorder für die gesamte Aufnahme. Niedrige Bitrate,
    // damit 20 Minuten deutlich unter dem Upload-Limit bleiben (~5 MB).
    const mediaRecorder = new MediaRecorder(stream, {
      audioBitsPerSecond: 32000,
    });
    mediaRecorderRef.current = mediaRecorder;
    chunksRef.current = [];
    recordedMsRef.current = 0;
    setElapsedMs(0);

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunksRef.current.push(e.data);
      // Im Pausenzustand den einen Player mit der bisherigen Tonspur aktualisieren
      if (mediaRecorder.state === "paused") updateAudioUrl();
    };

    mediaRecorder.start();
    startTimer();
    setIsRecording(true);
  };

  // Aufnahme pausieren: Recorder läuft weiter, es entsteht KEINE neue Tonspur
  const pause = () => {
    const recorder = mediaRecorderRef.current;
    if (!recorder || recorder.state !== "recording") return;

    recorder.pause();
    recorder.requestData(); // bisherige Daten für den Player holen
    stopTimer();
    stopVisualisation();
    setIsRecording(false);
    setState("review");
  };

  // Aufnahme fortsetzen: wird an dieselbe Tonspur angehängt
  const resume = () => {
    const recorder = mediaRecorderRef.current;
    if (!recorder || recorder.state !== "paused") return;
    if (recordedMsRef.current >= MAX_RECORDING_MS) return;

    recorder.resume();
    startTimer();
    startVisualisation();
    setIsRecording(true);
    setState("recording");
  };

  /** Beendet die Aufnahme endgültig und liefert die eine Tonspur. */
  const stopRecording = (): Promise<Blob> =>
    new Promise((resolve) => {
      const recorder = mediaRecorderRef.current;
      const finish = () => {
        streamRef.current?.getTracks().forEach((track) => track.stop());
        streamRef.current = null;
        resolve(buildBlob());
      };
      if (!recorder || recorder.state === "inactive") {
        finish();
        return;
      }
      recorder.onstop = finish;
      recorder.stop();
    });

  const finalizeRecording = () => {
    pause();
  };

  /**
   * Schickt die eine Tonspur an /speech-capture/transcribe
   * und gibt das Transkript zurück.
   */
  const transcribeAudio = async (): Promise<string> => {
    stopTimer();
    stopVisualisation();
    const blob = await stopRecording();

    if (blob.size === 0) {
      throw new Error("Keine Audioaufnahme vorhanden.");
    }

    const extension = blob.type.includes("mp4") ? "mp4" : "webm";
    const formData = new FormData();
    formData.append("audio", blob, `aufnahme.${extension}`);

    const response = await authorizedFetch(
      `${OFFER_SERVICE_URL}/speech-capture/transcribe`,
      {
        method: "POST",
        body: formData,
      },
    );

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(
        `Transkription fehlgeschlagen (${response.status}): ${errorText}`,
      );
    }

    const data = await response.json();
    return data.transkript as string;
  };

  const toggle = () => {
    if (state === "idle") {
      start();
    } else if (state === "recording") {
      pause();
    } else if (state === "review") {
      resume();
    }
  };

  const reset = () => {
    window.clearInterval(timerRef.current);
    stopVisualisation();
    const recorder = mediaRecorderRef.current;
    if (recorder && recorder.state !== "inactive") {
      recorder.ondataavailable = null;
      recorder.stop();
    }
    mediaRecorderRef.current = null;
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    chunksRef.current = [];
    recordedMsRef.current = 0;
    segmentStartRef.current = null;

    setState("idle");
    setIsRecording(false);
    setTranscript("");
    setElapsedMs(0);
    setAudioUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return null;
    });
    // Nur schließen, wenn noch nicht geschlossen — sonst wirft der Browser
    // "Can't close an AudioContext twice".
    if (audioContextRef.current && audioContextRef.current.state !== "closed") {
      audioContextRef.current.close();
    }
    audioContextRef.current = null;
    analyserRef.current = null;
  };

  return {
    isRecording,
    volume,
    toggle,
    transcript,
    setTranscript,
    audioUrl,
    elapsedMs,
    limitReached,
    state,
    reset,
    resume,
    pause,
    finalizeRecording,
    transcribeAudio,
  };
};
