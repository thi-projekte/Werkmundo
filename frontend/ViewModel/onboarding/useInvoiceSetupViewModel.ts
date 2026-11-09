import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  INVOICE_SETUP_STORAGE_KEY,
  initialInvoiceSetup,
  type InvoiceSetupForm,
} from "../../Model/onboarding/setupForms";

function readInvoiceSetup(): InvoiceSetupForm {
  const saved = sessionStorage.getItem(INVOICE_SETUP_STORAGE_KEY);
  if (!saved) return initialInvoiceSetup;

  try {
    return { ...initialInvoiceSetup, ...JSON.parse(saved) } as InvoiceSetupForm;
  } catch (error) {
    console.error("Die gespeicherten Rechnungsdaten konnten nicht gelesen werden:", error);
    return initialInvoiceSetup;
  }
}

export function useInvoiceSetupViewModel() {
  const navigate = useNavigate();
  const [form, setForm] = useState(readInvoiceSetup);
  const [error, setError] = useState("");

  const updateField = <K extends keyof InvoiceSetupForm>(
    field: K,
    value: InvoiceSetupForm[K],
  ) => setForm((current) => ({ ...current, [field]: value }));

  const finishSetup = () => {
    setError("");
    try {
      sessionStorage.setItem(INVOICE_SETUP_STORAGE_KEY, JSON.stringify(form));
      navigate("/dashboard");
    } catch (storageError) {
      console.error("Die Rechnungsdaten konnten nicht gespeichert werden:", storageError);
      setError("Deine Angaben konnten nicht gespeichert werden. Bitte versuche es erneut.");
    }
  };

  const skipSetup = () => navigate("/dashboard");

  return { error, finishSetup, form, skipSetup, updateField };
}
