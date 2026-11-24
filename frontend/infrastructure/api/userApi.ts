import { authorizedFetch, authorizedJson } from "../http/apiClient";

export type CustomerProfile = {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  companyName?: string;
  street?: string;
  houseNumber?: string;
  zipCode?: string;
  city?: string;
  state?: string;
  country?: string;
};

export type CustomerInput = Omit<CustomerProfile, "id">;

export type UserProfile = {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  companyName?: string;
  street?: string;
  houseNumber?: string;
  zipCode?: string;
  city?: string;
  country?: string;
  companyEmail?: string;
  roles?: string[];
  profilePictureUrl?: string;
};

// Basis inkl. /api/users (siehe .env.example und Dockerfile.frontend)
const userServiceUrl =
  import.meta.env.VITE_USER_SERVICE_URL || "http://localhost:8081/api/users";
const customersUrl = `${userServiceUrl}/customers`;

export function getCurrentUser(): Promise<UserProfile> {
  return authorizedJson<UserProfile>(`${userServiceUrl}/me`);
}

export async function uploadProfilePicture(file: File): Promise<string> {
  const body = new FormData();
  body.append("file", file);
  const response = await authorizedFetch(`${userServiceUrl}/profile-picture`, {
    method: "POST",
    body,
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const result = (await response.json()) as { url: string };
  return result.url;
}

export async function getProfilePicturePreview(path: string): Promise<string> {
  const response = await authorizedFetch(new URL(path, userServiceUrl).toString());
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return URL.createObjectURL(await response.blob());
}

export function getCustomers(): Promise<CustomerProfile[]> {
  return authorizedJson<CustomerProfile[]>(customersUrl);
}

export function getCustomer(id: number): Promise<CustomerProfile> {
  return authorizedJson<CustomerProfile>(`${customersUrl}/${id}`);
}

export function createCustomer(customer: CustomerInput): Promise<CustomerProfile> {
  return authorizedJson<CustomerProfile>(customersUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(customer),
  });
}

export function updateCustomer(
  id: number,
  customer: Partial<CustomerInput>,
): Promise<CustomerProfile> {
  return authorizedJson<CustomerProfile>(`${customersUrl}/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(customer),
  });
}

export async function deleteCustomer(id: number): Promise<void> {
  await authorizedJson<void>(`${customersUrl}/${id}`, { method: "DELETE" });
}
