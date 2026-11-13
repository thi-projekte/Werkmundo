import { authorizedJson } from "../http/apiClient";

export type CustomerProfile = {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
};

export type UserProfile = {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
};

const userServiceUrl =
  import.meta.env.VITE_USER_SERVICE_URL || "http://localhost:8081/api/users";

export function getCurrentUser(): Promise<UserProfile> {
  return authorizedJson<UserProfile>(`${userServiceUrl}/me`);
}

export function getCustomers(): Promise<CustomerProfile[]> {
  return authorizedJson<CustomerProfile[]>(`${userServiceUrl}/customers`);
}
