export type Employee = {
  group: string;
  hourlyRate: string;
};

export type CompanySetupForm = {
  companyName: string;
  legalForm: string;
  trade: string;
  email: string;
  contact: string;
  phone: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
  employees: Employee[];
};

export type InvoiceSetupForm = {
  vatTreatment: string;
  taxNumber: string;
  taxId: string;
  invoicePrefix: string;
  paymentTerm: string;
  invoiceEmail: string;
  accountHolder: string;
  iban: string;
};

export const COMPANY_SETUP_STORAGE_KEY = "werkmundo-company-setup";
export const INVOICE_SETUP_STORAGE_KEY = "werkmundo-invoice-setup";

export const initialCompanySetup: CompanySetupForm = {
  companyName: "",
  legalForm: "",
  trade: "",
  email: "",
  contact: "",
  phone: "",
  street: "",
  postalCode: "",
  city: "",
  country: "Deutschland",
  employees: [
    { group: "Geselle", hourlyRate: "75,00" },
    { group: "Meister", hourlyRate: "115,00" },
  ],
};

export const initialInvoiceSetup: InvoiceSetupForm = {
  vatTreatment: "",
  taxNumber: "",
  taxId: "",
  invoicePrefix: "RE-2026-",
  paymentTerm: "14 Tage",
  invoiceEmail: "",
  accountHolder: "",
  iban: "",
};
