import { apiRequest } from "../http/apiClient";

const offerServiceUrl = import.meta.env.VITE_OFFER_SERVICE_URL || "http://localhost:8082";

export type CreateOfferRequest = {
  customerId: string;
  speechSnippet: string;
  title?: string;
};

export type CreatedOffer = {
  id: number;
  businessKey: string;
};

/**
 * Legt ein Angebot an. Der offer-service startet damit den Prozess in der Process Engine,
 * die anschließend den ai-service (POST /ai/process) mit dem Sprachschnipsel aufruft.
 */
export function createOffer(request: CreateOfferRequest): Promise<CreatedOffer> {
  return apiRequest<CreatedOffer>(`${offerServiceUrl}/offers`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

/** Speichert einen Entwurf ohne Kunden (startet keinen Prozess). */
export function saveOfferDraft(request: { title: string; speechSnippet: string }): Promise<CreatedOffer> {
  return apiRequest<CreatedOffer>(`${offerServiceUrl}/offers/drafts`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}
