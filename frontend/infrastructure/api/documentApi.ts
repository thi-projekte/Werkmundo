import { authorizedFetch, authorizedJson, ApiError } from "../http/apiClient";
import type { OfferRecord } from "./offerApi";

export type InvoiceRecord = {
  id: number;
  rechnungsnummer: string;
  offerBusinessKey: string;
  gesamtPreis: number | string;
  createdAt: string;
  positions: InvoicePositionRecord[];
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

export type InvoicePositionRecord = {
  bezeichnung: string;
  menge: number | string;
  einheit?: string;
  einzelPreis: number | string;
  positionsPreis: number | string;
};

export type DocumentMetadata = {
  documentId: string;
  documentType?: string;
  fileName?: string;
};

export type InvoiceIssuer = {
  name: string;
  address: string;
  email: string;
};

export type OfferDocumentSnapshot = {
  documentDate: string;
  issuer: InvoiceIssuer;
  customer: InvoiceIssuer;
  items: {
    description: string;
    quantity: number;
    unit: string;
    unitPrice: number;
    lineTotal: number;
  }[];
  netAmount: number;
  taxLines: { label: string; rate: number; amount: number }[];
  totalAmount: number;
  currency: string;
  notes?: string;
};

export type CreatedDocument = {
  documentId: string;
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

export function generateInvoiceDocument(
  businessKey: string,
  invoice: InvoiceRecord,
  issuer: InvoiceIssuer,
): Promise<CreatedDocument> {
  return authorizedJson<CreatedDocument>(
    `${documentServiceUrl}/documents/invoices/${encodeURIComponent(businessKey)}/generate`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ rechnungsentwurf: { ...invoice, issuer } }),
    },
  );
}

export function generateOfferDocument(
  businessKey: string,
  snapshot: OfferDocumentSnapshot,
  customerId: string,
  recipientEmail: string,
): Promise<CreatedDocument> {
  return authorizedJson<CreatedDocument>(
    `${documentServiceUrl}/documents/offers/${encodeURIComponent(businessKey)}/generate`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ snapshot, customerId, recipientEmail }),
    },
  );
}

export async function previewOfferDocument(
  businessKey: string,
  snapshot: OfferDocumentSnapshot,
): Promise<Blob> {
  const response = await authorizedFetch(
    `${documentServiceUrl}/documents/offers/${encodeURIComponent(businessKey)}/preview`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/pdf" },
      body: JSON.stringify({ snapshot }),
    },
  );
  if (!response.ok) {
    throw new ApiError(`HTTP ${response.status}`, response.status);
  }
  return response.blob();
}

export async function fetchDocumentPdf(documentId: string): Promise<Blob> {
  const response = await authorizedFetch(
    `${documentServiceUrl}/documents/${encodeURIComponent(documentId)}/pdf?inline=true`,
    { headers: { Accept: "application/pdf" } },
  );
  if (!response.ok) {
    throw new ApiError(`HTTP ${response.status}`, response.status);
  }
  return response.blob();
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
