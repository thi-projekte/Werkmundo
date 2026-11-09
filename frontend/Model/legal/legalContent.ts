export type LegalSection = {
  heading: string;
  body: string;
  email?: string;
};

export const imprintSections: LegalSection[] = [
  {
    heading: "Betreiber",
    body: "(Studienprojekt)\nTechnische Hochschule Ingolstadt\nEsplanade 10\n85049 Ingolstadt\nDeutschland",
  },
  {
    heading: "Kontakt",
    body: "kontakt@projekt.de",
    email: "kontakt@projekt.de",
  },
  {
    heading: "Verantwortlich",
    body: "Projektteam",
  },
];

export const privacySections: LegalSection[] = [
  {
    heading: "1. Allgemeine Hinweise",
    body: "Diese Anwendung ist ein Studienprojekt der Technischen Hochschule Ingolstadt. Der Schutz personenbezogener Daten erfolgt gemäß DSGVO.",
  },
  {
    heading: "2. Authentifizierung",
    body: "Anmeldungen erfolgen über Keycloak zur sicheren Zugriffskontrolle.",
  },
  {
    heading: "3. Sprachverarbeitung & KI",
    body: "Sprachdaten können zur automatisierten Angebotserstellung verarbeitet werden.",
  },
  {
    heading: "4. Geschäftsdaten",
    body: "Daten werden lokal gespeichert und nicht an KI weitergegeben.",
  },
  {
    heading: "5. Sicherheit",
    body: "Alle Daten werden geschützt gespeichert und verarbeitet.",
  },
];
