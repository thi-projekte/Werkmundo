/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS: frontend/src/services/userService.ts
 * NEUE SCHICHT: infrastructure/api  (API-Client für den user-service; ersetzt den leeren
 *               Platzhalter infrastructure/api/userApi.ts)
 * -------------------------------------------------------------------------------------
 * BEREITS ERLEDIGT:
 *   Die fachlichen Typen UserStatus, UserRole, UserProfile, CustomerProfile wurden nach
 *   Beispiel/frontend/Model/user/UserProfile.ts verschoben und werden unten importiert und
 *   re-exportiert (damit bestehende Imports wie `type CustomerProfile` weiter funktionieren).
 *
 * WAS JETZT IN DIESER DATEI ZU TUN IST:
 *
 * 1. Imports anpassen (bewusst noch NICHT geändert, da im Beispiel-Ordner kein "@"-Alias
 *    konfiguriert ist):
 *      "@/services/authService" -> Beispiel/frontend/infrastructure/auth/authService.ts
 *
 * 2. Generische HTTP-Hilfsfunktionen aus dieser Datei herausziehen nach
 *    Beispiel/frontend/infrastructure/http/apiClient.ts (leerer Platzhalter, gemeinsam P + J):
 *      parseResponseBody(), extractErrorMessage(), handleResponse(), getAuthorizationHeader()
 *    Diese Logik ist in offerService.ts, documentApi.ts usw. (Squad J) mehrfach kopiert.
 *    -> Mit Squad J einen gemeinsamen apiClient (fetch-Wrapper mit Token + Fehlerbehandlung)
 *       abstimmen; Fehlertyp in Beispiel/frontend/shared/types/ApiError.ts definieren.
 *
 * 3. URL-Auflösung (USER_SERVICE_URL unten) in eine zentrale Konfiguration verschieben
 *    (im Original frontend/src/config/api.ts). Laut Architektur läuft alles über das
 *    API-Gateway -> langfristig nur noch EINE Basis-URL (Gateway) statt einer pro Service.
 *
 * 4. Kunden-Funktionen (createCustomer, getCustomers, updateCustomer, deleteCustomer, siehe
 *    Markierung unten) gehören zu Squad J -> nach Beispiel/frontend/infrastructure/api/
 *    (z.B. customerApi.ts) verschieben, sobald das Backend dafür feststeht.
 *
 * 5. Optional: Datei in userApi.ts umbenennen (Name des alten Platzhalters), damit die
 *    Benennung zu dashboardApi.ts / offerApi.ts passt.
 * =====================================================================================
 */
import { getToken } from "@/services/authService";
import type {
  CustomerProfile,
  UserProfile,
} from "../../Model/user/UserProfile";

export type {
  CustomerProfile,
  UserProfile,
  UserRole,
  UserStatus,
} from "../../Model/user/UserProfile";

const configuredUserServiceUrl =
  import.meta.env.VITE_USER_SERVICE_URL?.trim();

const configuredApiUrl = import.meta.env.VITE_API_URL?.trim();

export const USER_SERVICE_URL = (
  configuredUserServiceUrl ||
  (configuredApiUrl ? `${configuredApiUrl}/api/users` : "/api/users")
).replace(/\/+$/, "");

export type RegisterUserRequest = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
};

export type PasswordResetRequest = {
  email: string;
};

export type ProfileUpdateRequest = {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  profilePictureUrl?: string | null;
};

export type CompanyUpdateRequest = {
  companyName?: string;
  vatId?: string;
  tradeRegisterNumber?: string;

  street?: string;
  houseNumber?: string;
  zipCode?: string;
  city?: string;
  state?: string;
  country?: string;

  companyEmail?: string;
  companyPhoneNumber?: string;
  website?: string;
  industry?: string;

  iban?: string;
  bic?: string;
  bankName?: string;
  accountHolder?: string;

  taxNumber?: string;
  legalForm?: string;

  employeeCount?: number;
  customerCount?: number;
  hourlyRate?: number;

};

export type ProfilePictureUploadResponse = {
  url: string;
};

// TODO: -> infrastructure/http/apiClient.ts (gemeinsam mit Squad J, siehe Punkt 2 im Dateikopf)
const parseResponseBody = async <T>(response: Response): Promise<T | null> => {
  if (response.status === 204) {
    return null;
  }

  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as T;
  } catch {
    return text as T;
  }
};

const extractErrorMessage = async (response: Response): Promise<string> => {
  const fallbackMessage = `Request fehlgeschlagen (${response.status}).`;

  try {
    const text = await response.text();

    if (!text) {
      return fallbackMessage;
    }

    try {
      const parsed = JSON.parse(text) as {
        message?: string;
        error?: string;
        details?: string;
      };

      return (
        parsed.message ||
        parsed.error ||
        parsed.details ||
        fallbackMessage
      );
    } catch {
      return text;
    }
  } catch {
    return fallbackMessage;
  }
};

const handleResponse = async <T>(response: Response): Promise<T> => {
  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }

  return (await parseResponseBody<T>(response)) as T;
};

const getAuthorizationHeader = async (): Promise<Record<string, string>> => {
  const token = await getToken();

  if (!token) {
    throw new Error(
      "Keine gültige Anmeldung vorhanden. Bitte melde dich erneut an.",
    );
  }

  return {
    Authorization: `Bearer ${token}`,
  };
};

export const getProfilePictureUrl = (
  profilePictureUrl?: string | null,
): string | null => {
  if (!profilePictureUrl) {
    return null;
  }

  if (
    profilePictureUrl.startsWith("http://") ||
    profilePictureUrl.startsWith("https://") ||
    profilePictureUrl.startsWith("blob:") ||
    profilePictureUrl.startsWith("data:")
  ) {
    return profilePictureUrl;
  }

  try {
    const serviceUrl = new URL(USER_SERVICE_URL, window.location.origin);

    return new URL(profilePictureUrl, serviceUrl.origin).toString();
  } catch {
    return profilePictureUrl;
  }
};

export const registerUser = async (
  data: RegisterUserRequest,
): Promise<void> => {
  const response = await fetch(`${USER_SERVICE_URL}/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  await handleResponse<unknown>(response);
};

export const initiatePasswordReset = async (
  data: PasswordResetRequest,
): Promise<void> => {
  const response = await fetch(
    `${USER_SERVICE_URL}/password-reset/initiate`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    },
  );

  await handleResponse<unknown>(response);
};

export const getCurrentUser = async (): Promise<UserProfile> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/me`, {
    method: "GET",
    headers: authHeaders,
  });

  return handleResponse<UserProfile>(response);
};
export type HourlyRateResponse = {
  stundensatz: number;
};



export const updateProfile = async (
  data: ProfileUpdateRequest,
): Promise<UserProfile | null> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/profile`, {
    method: "PUT",
    headers: {
      ...authHeaders,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  return handleResponse<UserProfile | null>(response);
};

export const updateCompany = async (
  data: CompanyUpdateRequest,
): Promise<UserProfile | null> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/company`, {
    method: "PUT",
    headers: {
      ...authHeaders,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });

  return handleResponse<UserProfile | null>(response);
};

export const uploadProfilePicture = async (
  file: File,
): Promise<ProfilePictureUploadResponse> => {
  const authHeaders = await getAuthorizationHeader();

  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${USER_SERVICE_URL}/profile-picture`, {
    method: "POST",
    headers: authHeaders,
    body: formData,
  });

  return handleResponse<ProfilePictureUploadResponse>(response);
};

// ---------------------------------------------------------------------------------
// TODO [Squad J]: Ab hier bis Dateiende = Kunden-API. Gehört zu Squad J -> nach
// infrastructure/api/customerApi.ts (o.ä.) verschieben, siehe Punkt 4 im Dateikopf.
// ---------------------------------------------------------------------------------
export const createCustomer = async (
  data: Partial<CustomerProfile> & {
    email: string;
    firstName?: string;
    lastName?: string;
  },
): Promise<CustomerProfile> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/customers`, {
    method: "POST",
    headers: {
      ...authHeaders,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      ...data,

      firstName: data.firstName || "",
      lastName: data.lastName || "",
      phoneNumber: data.phoneNumber || null,
      companyName: data.companyName || null,

      // neue Adressfelder
      street: data.street || "",
      houseNumber: data.houseNumber || "",
      zipCode: data.zipCode || "",
      city: data.city || "",
      state: data.state || "",
      country: data.country || "Deutschland",
    }),
  });

  return handleResponse<CustomerProfile>(response);
};

export const getCustomers = async (): Promise<CustomerProfile[]> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(`${USER_SERVICE_URL}/customers`, {
    method: "GET",
    headers: authHeaders,
  });

  return handleResponse<CustomerProfile[]>(response);
};
export const updateCustomer = async (
  id: number,
  data: Partial<CustomerProfile>,
): Promise<CustomerProfile> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(
    `${USER_SERVICE_URL}/customers/${id}`,
    {
      method: "PUT",
      headers: {
        ...authHeaders,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    },
  );

  return handleResponse<CustomerProfile>(response);
};
export const deleteCustomer = async (id: number): Promise<void> => {
  const authHeaders = await getAuthorizationHeader();

  const response = await fetch(
    `${USER_SERVICE_URL}/customers/${id}`,
    {
      method: "DELETE",
      headers: authHeaders,
    },
  );

  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }
};