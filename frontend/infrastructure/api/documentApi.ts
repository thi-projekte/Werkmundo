import { authorizedFetch, authorizedJson, ApiError } from "../http/apiClient";
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

export type DocumentMetadata = {
  documentId: string;
  documentType?: string;
  fileName?: string;
};

const offerServiceUrl =
  import.meta.env.VITE_OFFER_SERVICE_URL || "http://localhost:8082";
const documentServiceUrl =
  import.meta.env.VITE_INVOICE_SERVICE_URL || import.meta.env.VITE_DOCUMENT_SERVICE_URL || "http://localhost:8084";

export function listDocuments(): Promise<DocumentMetadata[]> {
  return authorizedJson<DocumentMetadata[]>(`${documentServiceUrl}/documents`);
}

export function getDocumentOffers(): Promise<OfferRecord[]> {
  return authorizedJson<OfferRecord[]>(`${offerServiceUrl}/offers`);
}

export function getInvoices(): Promise<InvoiceRecord[]> {
  return authorizedJson<InvoiceRecord[]>(`${offerServiceUrl}/rechnungen`);
}

export function createInvoice(businessKey: string): Promise<InvoiceRecord> {
  return authorizedJson<InvoiceRecord>(`${offerServiceUrl}/rechnungen`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ businessKey }),
  });
}

export async function shareOffer(businessKey: string): Promise<void> {
  const response = await authorizedFetch(
    `${documentServiceUrl}/documents/offers/${encodeURIComponent(businessKey)}/share`,
    { method: "POST" },
  );
  if (!response.ok) {
    throw new ApiError(`HTTP ${response.status}`, response.status);
  }
}
