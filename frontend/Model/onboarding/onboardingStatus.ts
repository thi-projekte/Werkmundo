export const ONBOARDING_STATUS_KEY = "werkmundo-onboarding-status";

export type OnboardingStatus = {
  company: boolean;
  employees: boolean;
  invoices: boolean;
};

const initialOnboardingStatus: OnboardingStatus = {
  company: false,
  employees: false,
  invoices: false,
};

export function readOnboardingStatus(): OnboardingStatus {
  try {
    const stored = localStorage.getItem(ONBOARDING_STATUS_KEY);
    if (!stored) return initialOnboardingStatus;
    const parsed = JSON.parse(stored) as Partial<OnboardingStatus>;
    return {
      company: parsed.company === true,
      employees: parsed.employees === true,
      invoices: parsed.invoices === true,
    };
  } catch (error) {
    console.error("Der Einrichtungsstatus konnte nicht gelesen werden:", error);
    return initialOnboardingStatus;
  }
}

export function updateOnboardingStatus(
  update: Partial<OnboardingStatus>,
): OnboardingStatus {
  const next = { ...readOnboardingStatus(), ...update };
  localStorage.setItem(ONBOARDING_STATUS_KEY, JSON.stringify(next));
  return next;
}

export function completedOnboardingSteps(status: OnboardingStatus): number {
  return Number(status.company) + Number(status.employees) + Number(status.invoices);
}
