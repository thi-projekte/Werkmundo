import { ContactRound } from "lucide-react";
import { PlaceholderPage } from "../shared/navigation/PlaceholderPage";

export function CustomerListView() {
  return (
    <PlaceholderPage
      title="Deine"
      accent="Kunden"
      icon={ContactRound}
      description="Hier kannst du bald deine Kunden ansehen, anlegen und bearbeiten."
    />
  );
}
