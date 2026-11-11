import type { TranscriptionResult } from "../../Model/voice/VoiceRecording";
import { apiRequest } from "../http/apiClient";

const aiServiceUrl = import.meta.env.VITE_AI_SERVICE_URL || "http://localhost:8086";

/** Schickt die Aufnahme an den ai-service und liefert das Transkript. */
export async function transcribeAudio(audio: Blob): Promise<string> {
  const extension = audio.type.includes("mp4") ? "m4a" : audio.type.includes("ogg") ? "ogg" : "webm";
  const formData = new FormData();
  formData.append("audio", audio, `aufnahme.${extension}`);

  const result = await apiRequest<TranscriptionResult>(`${aiServiceUrl}/ai/transcribe`, {
    method: "POST",
    body: formData,
  });
  return result.transkript;
}
