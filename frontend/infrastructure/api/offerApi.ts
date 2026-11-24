import { authorizedFetch } from "../http/apiClient";

export type OfferPosition = {
  id?: number;
  hersteller?: string;
  bezeichnung: string;
  beschreibung?: string;
  menge: number | string;
  einheit: string;
  einzelPreis: number | string;
  positionsPreis?: number | string;
  reihenfolge?: number;
  type?: string;
};

export type OfferRecord = {
  id: number;
  businessKey: string;
  offerNumber?: string | null;
  customerId?: string;
  title?: string;
  status: string;
  speechSnippet?: string;
  positions?: OfferPosition[];
  createdAt?: string;
  updatedAt?: string;
  gesamtPreis?: number | string;
};

export type CreateOfferInput = {
  customerId: string;
  speechSnippet: string;
  title?: string;
};

export type SaveOfferDraftInput = {
  title: string;
  speechSnippet: string;
};

const offerServiceUrl =
  import.meta.env.VITE_OFFER_SERVICE_URL || "http://localhost:8082";

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await authorizedFetch(`${offerServiceUrl}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as T;
}

export function createOffer(input: CreateOfferInput): Promise<OfferRecord> {
  return postJson<OfferRecord>("/offers", input);
}

export function saveOfferDraft(
  input: SaveOfferDraftInput,
): Promise<OfferRecord> {
  return postJson<OfferRecord>("/offers/drafts", input);
}

export async function getOffers(): Promise<OfferRecord[]> {
  const response = await authorizedFetch(`${offerServiceUrl}/offers`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as OfferRecord[];
}

export async function getOffer(businessKey: string): Promise<OfferRecord> {
  const response = await authorizedFetch(
    `${offerServiceUrl}/offers/${encodeURIComponent(businessKey)}`,
  );
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as OfferRecord;
}

export async function assignOfferCustomer(
  businessKey: string,
  customerId: number,
): Promise<OfferRecord> {
  const response = await authorizedFetch(
    `${offerServiceUrl}/offers/${encodeURIComponent(businessKey)}/customer`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ customerId }),
    },
  );

  if (!response.ok) {
    throw new Error(
      `Kundenzuordnung fehlgeschlagen (HTTP ${response.status})`,
    );
  }

  return (await response.json()) as OfferRecord;
}

export type DraftPositionInput = {
  bezeichnung: string;
  menge: number;
  einheit: string;
  einzelPreis: number;
};

export async function updateDraftPositions(
  businessKey: string,
  positions: DraftPositionInput[],
): Promise<OfferRecord> {
  const response = await authorizedFetch(
    `${offerServiceUrl}/offers/${encodeURIComponent(businessKey)}/draft-positions`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ positions }),
    },
  );

  if (!response.ok) {
    throw new Error(
      `Angebotspositionen konnten nicht gespeichert werden (HTTP ${response.status})`,
    );
  }

  return (await response.json()) as OfferRecord;
}