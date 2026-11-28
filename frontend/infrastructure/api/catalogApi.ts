import { authorizedFetch } from "../http/apiClient";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await authorizedFetch(`/svc/catalog/catalog/material${path}`, init);
  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null);
    const message = body && typeof body === "object" && "message" in body && typeof body.message === "string"
      ? body.message
      : response.status === 413 ? "Die Datei ist zu groß. Bitte wähle eine CSV-Datei unter 10 MB."
      : `Kataloganfrage fehlgeschlagen (HTTP ${response.status}).`;
    throw new Error(message);
  }
  return response.json() as Promise<T>;
}

export function getCatalogStatus() {
  return request<{ articleCount: number }>("/status");
}
export function uploadCatalog(file: File) {
  const body = new FormData();
  body.append("file", file);
  return request<number>("/import/csv", { method: "POST", body });
}
