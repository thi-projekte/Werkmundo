import { FileText } from "lucide-react";
import { PlaceholderPage } from "../shared/navigation/PlaceholderPage";

export function InvoiceView() {
  return (
    <PlaceholderPage
      title="Deine"
      accent="Rechnungen"
      icon={FileText}
      description="Hier erscheinen bald alle Rechnungen mit Status und PDF-Download."
    />
  );
}
