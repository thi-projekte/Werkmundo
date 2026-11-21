/*
 * [Squad P] KOPIERT AUS: frontend/src/features/voice-input/components/MicButton.tsx
 * NEUE SCHICHT: View (reine Darstellung, bekommt State + Callback per Props) - MVVM-konform.
 * TODO: Import "@/assets/icons/mic.svg" -> Assets wurden nicht kopiert, liegen weiter in
 *       frontend/src/assets (gemeinsam P + J). Pfad nach Zusammenführung prüfen.
 */
import MicIcon from "@/assets/icons/mic.svg";

type Props = {
  isRecording: boolean;
  volume: number;
  onClick: () => void;
};

export const MicButton = ({ isRecording, volume, onClick }: Props) => {
  return (
    <button
      className={`mic-button ${isRecording ? "active" : ""}`}
      onClick={onClick}
      style={{
        transform: `scale(${1 + volume / 200})`,
      }}
    >
      <img src={MicIcon} alt="Microphone" />
    </button>
  );
};