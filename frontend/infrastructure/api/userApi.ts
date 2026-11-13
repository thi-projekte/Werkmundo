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

const userServiceUrl =
  import.meta.env.VITE_USER_SERVICE_URL || "http://localhost:8081";
const customersUrl = `${userServiceUrl}/api/users/customers`;

export async function getCustomers(): Promise<CustomerProfile[]> {
  const response = await fetch(customersUrl);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as CustomerProfile[];
}

export async function getCustomer(id: number): Promise<CustomerProfile> {
  const response = await fetch(`${customersUrl}/${id}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as CustomerProfile;
}

export async function createCustomer(
  customer: CustomerInput,
): Promise<CustomerProfile> {
  const response = await fetch(customersUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(customer),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as CustomerProfile;
}

export async function updateCustomer(
  id: number,
  customer: Partial<CustomerInput>,
): Promise<CustomerProfile> {
  const response = await fetch(`${customersUrl}/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(customer),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as CustomerProfile;
}

export async function deleteCustomer(id: number): Promise<void> {
  const response = await fetch(`${customersUrl}/${id}`, { method: "DELETE" });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
}
