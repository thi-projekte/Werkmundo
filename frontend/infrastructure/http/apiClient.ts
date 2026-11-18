import keycloak, { initKeycloak, isKeycloakConfigured } from "../auth/keycloakClient";

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export async function authorizedFetch(url: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);

  if (isKeycloakConfigured()) {
    await initKeycloak();
    if (keycloak.authenticated) {
      await keycloak.updateToken(30);
      if (keycloak.token) {
        headers.set("Authorization", `Bearer ${keycloak.token}`);
      }
    }
  }

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  return fetch(url, { ...init, headers });
}

export async function authorizedJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await authorizedFetch(url, init);
  if (!response.ok) {
    throw new ApiError(`HTTP ${response.status}`, response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
