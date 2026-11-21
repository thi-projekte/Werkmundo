/*
 * =====================================================================================
 * [Squad P] HERAUSGELÖST AUS: frontend/src/services/userService.ts (Typ-Definitionen)
 * NEUE SCHICHT: Model  (reine Daten-States, keine Logik, keine API-Aufrufe)
 * -------------------------------------------------------------------------------------
 * Die fachlichen Typen (UserStatus, UserRole, UserProfile, CustomerProfile) wurden aus
 * infrastructure/api/userService.ts hierher verschoben. Request-/Response-DTOs der API
 * (RegisterUserRequest, ProfileUpdateRequest, ...) sind bewusst in userService.ts geblieben,
 * weil sie den HTTP-Vertrag beschreiben und nicht den Daten-State der App.
 *
 * WAS JETZT IN DIESER DATEI ZU TUN IST:
 *
 * 1. UserProfile mischt Person und Firma. Aufteilen in:
 *      Model/user/UserProfile.ts   -> id, email, firstName, lastName, phoneNumber,
 *                                     profilePictureUrl, status, roles
 *      Model/user/Company.ts (neu) -> companyName ... disclaimer (Firma, Adresse, Bank,
 *                                     Steuer, Stundensatz, KI-Stil)
 *    Der leere Platzhalter Model/user/User.ts kann dafür als Basis-Typ (id, email, Name)
 *    genutzt oder gelöscht werden.
 *
 * 2. Feldnamen mit dem Backend abgleichen (siehe Beispiel/backend/services/user-service/
 *    domain/UserEntity.java): Backend heißt "paymentTerms"/"agbNotes", hier
 *    "termsOfPayment"/"disclaimer"; travelModel/travelFlatRate/travelKmRate fehlen hier.
 *
 * 3. CustomerProfile ist nur ein Alias auf UserProfile. Kunden gehören laut Architektur zu
 *    Squad J -> Typ nach Beispiel/frontend/Model/customer/Customer.ts verschieben
 *    (Platzhalter existiert) und mit Squad J abstimmen.
 *
 * 4. Die Views/ViewModels importieren diese Typen aktuell noch aus "@/services/userService".
 *    Nach dem Umzug Imports auf diese Datei umstellen.
 * =====================================================================================
 */

export type UserStatus = "PENDING" | "ACTIVE" | "DELETED";

export type UserRole =
  | "OWNER"
  | "EMPLOYEE"
  | "ACCOUNTANT"
  | "CUSTOMER"
  | string;

export type UserProfile = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string | null;
  profilePictureUrl?: string | null;
  status: UserStatus;
  roles: UserRole[];

  companyName?: string | null;
  vatId?: string | null;
  tradeRegisterNumber?: string | null;

  street?: string | null;
  houseNumber?: string | null;
  zipCode?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;

  companyEmail?: string | null;
  companyPhoneNumber?: string | null;
  website?: string | null;
  industry?: string | null;

  iban?: string | null;
  bic?: string | null;
  bankName?: string | null;
  accountHolder?: string | null;

  taxNumber?: string | null;
  legalForm?: string | null;

  employeeCount?: number | null;
  customerCount?: number | null;
  hourlyRate?: number | null;
  priceListUrl?: string | null;

  toneOfVoice?: string | null;
  detailLevel?: string | null;
  termsOfPayment?: string | null;
  disclaimer?: string | null;
};

// TODO [Squad J]: -> Model/customer/Customer.ts
export type CustomerProfile = UserProfile;
