export const OFFER_PLANS = [
  { id: "kompakt", label: "KOMPAKT", offers: 20, price: "39,99", description: "Für den Einstieg und gelegentliche Angebote." },
  { id: "alltag", label: "ALLTAG", offers: 40, price: "54,99", description: "Für Betriebe mit regelmäßigem Angebotsbedarf." },
  { id: "vielnutzer", label: "VIELNUTZER", offers: 60, price: "67,99", description: "Für Betriebe mit hohem Angebotsvolumen." },
] as const;

export type PlanId = (typeof OFFER_PLANS)[number]["id"];

export const PLAN_STORAGE_KEY = "werkmundo-selected-plan";

export function getStoredPlan(): PlanId {
  try {
    const stored = sessionStorage.getItem(PLAN_STORAGE_KEY);
    return OFFER_PLANS.find((plan) => plan.id === stored)?.id ?? "alltag";
  } catch {
    return "alltag";
  }
}
