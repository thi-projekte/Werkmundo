/**
 * Gemeinsamer HTTP-Client für alle Services (Squad P + J).
 * Hängt – sobald die Keycloak-Anbindung steht – automatisch das Access-Token an.
 */

type TokenProvider = () => Promise<string | null | undefined>;

let tokenProvider: TokenProvider | null = null;

/** Wird von der Auth-Schicht (keycloakClient) gesetzt, sobald ein Login existiert. */
export function setAccessTokenProvider(provider: TokenProvider | null) {
  tokenProvider = provider;
}

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");
  try {
    const parsed = JSON.parse(text) as { error?: string; message?: string };
    return parsed.error || parsed.message || text;
  } catch {
    return text;
  }
}

export async function apiRequest<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = tokenProvider ? await tokenProvider() : null;
  if (token) headers.set("Authorization", `Bearer ${token}`);
  // Bei FormData KEIN Content-Type setzen – der Browser ergänzt die multipart-Boundary selbst.
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  let response: Response;
  try {
    response = await fetch(url, { ...init, headers });
  } catch {
    throw new ApiRequestError("Der Server ist nicht erreichbar. Bitte prüfe deine Verbindung.", 0);
  }

  if (!response.ok) {
    const detail = await readErrorMessage(response);
    throw new ApiRequestError(detail || `Anfrage fehlgeschlagen (HTTP ${response.status}).`, response.status);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
