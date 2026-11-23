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

export type OfferPdfDraft = {
  businessKey: string;
  title?: string;
  customer: {
    name: string;
    street?: string;
    postalCode?: string;
    city?: string;
    email?: string;
  };
  seller?: {
    companyName?: string;
    contact?: string;
    email?: string;
    street?: string;
    postalCode?: string;
    city?: string;
  };
  positions: Array<{
    bezeichnung: string;
    beschreibung?: string;
    menge: number | string;
    einheit: string;
    einzelPreis?: number | string | null;
  }>;
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

async function requestOfferPdf(path: string, draft: OfferPdfDraft): Promise<Blob> {
  const response = await authorizedFetch(`${documentServiceUrl}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(draft),
  });
  if (!response.ok) {
    throw new ApiError(`PDF-Erzeugung fehlgeschlagen (HTTP ${response.status})`, response.status);
  }
  return response.blob();
}

export function getOfferPdf(businessKey: string, draft: OfferPdfDraft): Promise<Blob> {
  return requestOfferPdf(
    `/documents/offers/${encodeURIComponent(businessKey)}/pdf`,
    draft,
  );
}

export function getDemoOfferPdf(draft: OfferPdfDraft): Promise<Blob> {
  return requestOfferPdf("/documents/offers/demo/pdf", draft);
}
