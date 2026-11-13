import type { OfferRecord } from "./offerApi";

export type InvoiceRecord = {
  id: number;
  rechnungsnummer: string;
  offerBusinessKey: string;
  gesamtPreis: number | string;
  createdAt: string;
  kundendaten?: {
    vorname?: string;
    nachname?: string;
    email?: string;
    strasse?: string;
    hausnummer?: string;
    plz?: string;
    ort?: string;
  };
};

const offerServiceUrl =
  import.meta.env.VITE_OFFER_SERVICE_URL || "http://localhost:8082";
const documentServiceUrl =
  import.meta.env.VITE_DOCUMENT_SERVICE_URL || "http://localhost:8083";

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as T;
}

export function getDocumentOffers(): Promise<OfferRecord[]> {
  return getJson<OfferRecord[]>(`${offerServiceUrl}/offers`);
}

export function getInvoices(): Promise<InvoiceRecord[]> {
  return getJson<InvoiceRecord[]>(`${offerServiceUrl}/rechnungen`);
}

export async function createInvoice(businessKey: string): Promise<InvoiceRecord> {
  const response = await fetch(`${offerServiceUrl}/rechnungen`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ businessKey }),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as InvoiceRecord;
}

export async function shareOffer(businessKey: string): Promise<void> {
  const response = await fetch(
    `${documentServiceUrl}/documents/offers/${encodeURIComponent(businessKey)}/share`,
    { method: "POST" },
  );
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
}