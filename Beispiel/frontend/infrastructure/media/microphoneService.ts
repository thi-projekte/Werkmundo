/*
 * [Squad P] KOPIERT AUS: frontend/src/features/voice-input/services/microphoneService.ts
 * NEUE SCHICHT: infrastructure/media  (neuer Ordner: Zugriff auf Browser-Hardware/-APIs)
 * TODO:
 *   1. Hier später auch die AudioContext-/MediaRecorder-Logik aus
 *      ViewModel/voice/useVoiceInput.ts ablegen (siehe dort Punkt 1a).
 *   2. Fehlerfall (Nutzer verweigert Mikrofon -> NotAllowedError) abfangen und eine
 *      verständliche Fehlermeldung an das ViewModel zurückgeben.
 *   3. Streams nach dem Aufnehmen stoppen (stream.getTracks().forEach(t => t.stop())),
 *      sonst bleibt das Mikrofon-Symbol im Browser aktiv - Funktion stopMicrophone() ergänzen.
 */
export const startMicrophone = async () => {
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  return stream;
};