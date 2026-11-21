import MicIcon from "@/assets/icons/mic.svg";

type Props = {
  isRecording: boolean;
  volume: number;
  onClick: () => void;
  disabled?: boolean;
  label?: string;
};

export const MicButton = ({
  isRecording,
  volume,
  onClick,
  disabled = false,
  label = "Aufnahme starten",
}: Props) => {
  return (
    <button
      type="button"
      className={`mic-button ${isRecording ? "active" : ""}`}
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      style={{
        // Lautstärke (0–255) leicht sichtbar machen, max. +30 %
        transform: `scale(${1 + Math.min(volume, 255) / 850})`,
      }}
    >
      <img src={MicIcon} alt="" />
    </button>
  );
};
