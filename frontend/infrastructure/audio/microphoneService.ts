/**
 * Zugriff auf Mikrofon und MediaRecorder des Browsers.
 * Liegt in der infrastructure-Schicht, damit ViewModel und View keine Browser-APIs direkt anfassen.
 *
 * Pausieren/Fortsetzen läuft über MediaRecorder.pause()/resume() auf EINEM Recorder. So entsteht
 * eine durchgehende, gültige Audiodatei. (Mehrere einzeln gestoppte WebM-Dateien aneinander-
 * zuhängen ergibt keine gültige Datei – die Spracherkennung würde nur das erste Stück verstehen.)
 */

export type MicrophoneSession = {
  /** Pausiert die Aufnahme und liefert den bisherigen Stand als abspielbares Blob. */
  pause: () => Promise<Blob>;
  /** Setzt eine pausierte Aufnahme fort. */
  resume: () => void;
  /** Beendet die Aufnahme, gibt das Mikrofon frei und liefert die komplette Aufnahme. */
  stop: () => Promise<Blob>;
  /** Gibt das Mikrofon frei, ohne Daten zu liefern (Verwerfen). */
  cancel: () => void;
  /** Aktueller Pegel 0–100 (für die Animation des Mikrofon-Buttons). */
  readLevel: () => number;
};

const PREFERRED_MIME_TYPES = ["audio/webm;codecs=opus", "audio/webm", "audio/mp4", "audio/ogg"];

function pickMimeType(): string | undefined {
  if (typeof MediaRecorder === "undefined" || !MediaRecorder.isTypeSupported) return undefined;
  return PREFERRED_MIME_TYPES.find((type) => MediaRecorder.isTypeSupported(type));
}

export function isMicrophoneSupported(): boolean {
  return (
    typeof navigator !== "undefined" &&
    !!navigator.mediaDevices?.getUserMedia &&
    typeof MediaRecorder !== "undefined"
  );
}

/** Übersetzt Browser-Fehler beim Mikrofonzugriff in verständliche Meldungen. */
export function describeMicrophoneError(error: unknown): string {
  if (error instanceof DOMException) {
    if (error.name === "NotAllowedError" || error.name === "SecurityError") {
      return "Der Zugriff auf das Mikrofon wurde verweigert. Bitte erlaube ihn in den Browser-Einstellungen.";
    }
    if (error.name === "NotFoundError") {
      return "Es wurde kein Mikrofon gefunden.";
    }
    if (error.name === "NotReadableError") {
      return "Das Mikrofon wird gerade von einer anderen Anwendung verwendet.";
    }
  }
  return error instanceof Error ? error.message : "Das Mikrofon konnte nicht gestartet werden.";
}

/** Startet Mikrofon + MediaRecorder + Pegelmessung. */
export async function startRecording(): Promise<MicrophoneSession> {
  if (!isMicrophoneSupported()) {
    throw new Error(
      "Dein Browser unterstützt keine Sprachaufnahme. Bitte nutze einen aktuellen Browser über HTTPS oder localhost.",
    );
  }

  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  const mimeType = pickMimeType();
  const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
  const chunks: Blob[] = [];
  let pendingFlush: (() => void) | null = null;

  recorder.ondataavailable = (event) => {
    if (event.data.size > 0) chunks.push(event.data);
    pendingFlush?.();
    pendingFlush = null;
  };

  const audioContext = new AudioContext();
  const analyser = audioContext.createAnalyser();
  analyser.fftSize = 256;
  audioContext.createMediaStreamSource(stream).connect(analyser);
  const levels = new Uint8Array(analyser.frequencyBinCount);

  recorder.start();

  const currentBlob = () => new Blob(chunks, { type: recorder.mimeType || mimeType || "audio/webm" });

  const release = () => {
    stream.getTracks().forEach((track) => track.stop());
    if (audioContext.state !== "closed") void audioContext.close();
  };

  return {
    readLevel: () => {
      analyser.getByteFrequencyData(levels);
      const average = levels.reduce((sum, value) => sum + value, 0) / levels.length;
      return Math.min(100, Math.round((average / 255) * 100 * 2));
    },
    pause: () =>
      new Promise<Blob>((resolve) => {
        if (recorder.state !== "recording") {
          resolve(currentBlob());
          return;
        }
        recorder.pause();
        // Bisher gepufferte Daten anfordern, damit die Vorschau den aktuellen Stand enthält.
        pendingFlush = () => resolve(currentBlob());
        recorder.requestData();
      }),
    resume: () => {
      if (recorder.state === "paused") recorder.resume();
    },
    stop: () =>
      new Promise<Blob>((resolve) => {
        const finish = () => {
          release();
          resolve(currentBlob());
        };
        if (recorder.state === "inactive") {
          finish();
          return;
        }
        recorder.onstop = finish;
        recorder.stop();
      }),
    cancel: () => {
      recorder.onstop = null;
      if (recorder.state !== "inactive") recorder.stop();
      release();
    },
  };
}
