export type CustomerProfile = {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
};

const userServiceUrl =
  import.meta.env.VITE_USER_SERVICE_URL || "http://localhost:8081";

export async function getCustomers(): Promise<CustomerProfile[]> {
  const response = await fetch(`${userServiceUrl}/customers`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as CustomerProfile[];
}
