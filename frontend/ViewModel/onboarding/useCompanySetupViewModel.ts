import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  COMPANY_SETUP_STORAGE_KEY,
  initialCompanySetup,
  type CompanySetupForm,
} from "../../Model/onboarding/setupForms";
import { updateOnboardingStatus } from "../../Model/onboarding/onboardingStatus";

function readCompanySetup(): CompanySetupForm {
  const saved = sessionStorage.getItem(COMPANY_SETUP_STORAGE_KEY);
  if (!saved) return initialCompanySetup;

  try {
    return { ...initialCompanySetup, ...JSON.parse(saved) } as CompanySetupForm;
  } catch (error) {
    console.error("Die gespeicherten Unternehmensdaten konnten nicht gelesen werden:", error);
    return initialCompanySetup;
  }
}

export function useCompanySetupViewModel() {
  const navigate = useNavigate();
  const [form, setForm] = useState(readCompanySetup);
  const [error, setError] = useState("");

  const updateField = <K extends keyof CompanySetupForm>(
    field: K,
    value: CompanySetupForm[K],
  ) => setForm((current) => ({ ...current, [field]: value }));

  const updateEmployee = (
    index: number,
    field: "group" | "hourlyRate",
    value: string,
  ) => {
    setForm((current) => ({
      ...current,
      employees: current.employees.map((employee, employeeIndex) =>
        employeeIndex === index ? { ...employee, [field]: value } : employee,
      ),
    }));
  };

  const addEmployee = () =>
    setForm((current) => ({
      ...current,
      employees: [...current.employees, { group: "Geselle", hourlyRate: "" }],
    }));

  const continueToEmployees = () => {
    setError("");
    try {
      sessionStorage.setItem(COMPANY_SETUP_STORAGE_KEY, JSON.stringify(form));
      updateOnboardingStatus({ company: true });
      navigate("/einrichtung/mitarbeiter");
    } catch (storageError) {
      console.error("Die Unternehmensdaten konnten nicht gespeichert werden:", storageError);
      setError("Deine Angaben konnten nicht gespeichert werden. Bitte versuche es erneut.");
    }
  };

  const skipSetup = () => navigate("/einrichtung/mitarbeiter");

  const removeEmployee = (index: number) =>
    setForm((current) => ({
      ...current,
      employees: current.employees.filter((_, employeeIndex) => employeeIndex !== index),
    }));

  const continueToInvoices = () => {
    setError("");
    try {
      sessionStorage.setItem(COMPANY_SETUP_STORAGE_KEY, JSON.stringify(form));
      updateOnboardingStatus({ employees: true });
      navigate("/einrichtung/rechnungen");
    } catch (storageError) {
      console.error("Die Mitarbeiterdaten konnten nicht gespeichert werden:", storageError);
      setError("Deine Angaben konnten nicht gespeichert werden. Bitte versuche es erneut.");
    }
  };

  const skipEmployeeSetup = () => navigate("/einrichtung/rechnungen");

  return {
    addEmployee,
    continueToEmployees,
    continueToInvoices,
    skipEmployeeSetup,
    removeEmployee,
    skipSetup,
    error,
    form,
    updateEmployee,
    updateField,
  };
}
