import { authorizedJson } from "../http/apiClient";

const documentServiceUrl =
  import.meta.env.VITE_DOCUMENT_SERVICE_URL || "http://localhost:8084";

export type DocumentMetadata = {
  documentId: string;
  documentType?: string;
  fileName?: string;
};

export function listDocuments(): Promise<DocumentMetadata[]> {
  return authorizedJson<DocumentMetadata[]>(`${documentServiceUrl}/documents`);
}
