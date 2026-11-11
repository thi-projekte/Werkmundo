/**
 * Daten-States der Sprachaufnahme (Model im MVVM).
 *
 * idle         → noch nichts aufgenommen
 * recording    → Mikrofon läuft
 * paused       → Aufnahme angehalten, Segmente können angehört / fortgesetzt werden
 * transcribing → Aufnahme wird an den ai-service geschickt
 * transcribed  → Transkript liegt vor und kann geprüft / bearbeitet werden
 * submitting   → Angebot wird im offer-service angelegt
 */
export type RecordingState =
  | "idle"
  | "recording"
  | "paused"
  | "transcribing"
  | "transcribed"
  | "submitting";

export type InputMode = "voice" | "text";

/** Ein zusammenhängendes Stück Aufnahme (zwischen Start/Fortsetzen und Pause). */
export type AudioSegment = {
  blob: Blob;
  url: string;
};

/** Antwort von POST /ai/transcribe (ai-service). */
export type TranscriptionResult = {
  transkript: string;
};

/** Daten, die das Dashboard beim Wechsel auf die Aufnahme-Seite mitgibt. */
export type VoiceInputRouteState = {
  mode?: InputMode;
  customerId?: string;
  transcript?: string;
};

/** Ab dieser Länge (Sekunden) wird die Aufnahme automatisch pausiert. */
export const MAX_RECORDING_SECONDS = 5 * 60;
