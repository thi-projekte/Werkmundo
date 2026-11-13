import { CircleUserRound } from "lucide-react";
import { PlaceholderPage } from "../shared/navigation/PlaceholderPage";

export function ProfileView() {
  return (
    <PlaceholderPage
      title="Dein"
      accent="Profil"
      icon={CircleUserRound}
      description="Hier verwaltest du bald deine Benutzer- und Unternehmensdaten."
    />
  );
}
