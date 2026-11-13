import { authorizedJson } from "../http/apiClient";

export type DashboardStatsResponse = {
  nichtFertiggestellt: number;
  ohneRueckmeldung: number;
  mitRueckmeldung: number;
  angeboteGesamt: number;
  aufmerksamkeitErforderlich: Array<{
    offerId: number;
    businessKey: string;
    versendetAm: string;
  }>;
  letzteAktivitaeten: Array<{
    offerId: number;
    businessKey: string;
    zeitpunkt: string;
    status: string;
  }>;
};

const offerServiceUrl =
  import.meta.env.VITE_OFFER_SERVICE_URL || "http://localhost:8082";

export function getDashboardStats(): Promise<DashboardStatsResponse> {
  return authorizedJson<DashboardStatsResponse>(`${offerServiceUrl}/dashboard/stats`);
}
