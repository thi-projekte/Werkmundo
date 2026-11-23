import type { CSSProperties } from "react";
import { useNavigate } from "react-router-dom";
import {
  completedOnboardingSteps,
  readOnboardingStatus,
} from "../../Model/onboarding/onboardingStatus";

type SetupProgressProps = {
  activeStep?: 1 | 2 | 3;
  compact?: boolean;
};

export function SetupProgress({ activeStep, compact = false }: SetupProgressProps) {
  const navigate = useNavigate();
  const status = readOnboardingStatus();
  const completed = completedOnboardingSteps(status);
  const percent = Math.round((completed / 3) * 100);

  if (!activeStep && completed === 3) return null;

  const continueRoute = activeStep
    ? activeStep === 1
      ? "/einrichtung/unternehmen"
      : activeStep === 2
        ? "/einrichtung/mitarbeiter"
        : "/einrichtung/rechnungen"
    : !status.company
      ? "/einrichtung/unternehmen"
      : !status.employees
        ? "/einrichtung/mitarbeiter"
        : "/einrichtung/rechnungen";

  return (
    <aside
      className={`wm-setup-progress ${compact ? "is-compact" : ""}`}
      aria-label={`Einrichtung zu ${percent} Prozent abgeschlossen`}
    >
      <button
        type="button"
        onClick={() => navigate(continueRoute)}
        aria-label="Einrichtung fortsetzen"
      >
        <span
          className="wm-setup-progress-ring"
          style={{ "--wm-progress": `${percent * 3.6}deg` } as CSSProperties}
        >
          <strong>{completed}/3</strong>
        </span>
      </button>
      {!compact && (
        <div>
          <strong>Einrichtung zu {percent} % abgeschlossen</strong>
          <small>Du kannst die Einrichtung jederzeit fortsetzen.</small>
        </div>
      )}
    </aside>
  );
}
