/*
 * [Squad P] KOPIERT AUS: frontend/src/features/dashboard/components/StatusBadge.tsx
 * NEUE SCHICHT: View (reine Darstellung)
 * TODO: Die Status-Strings "offen" | "antwort" | "unfertig" als Union-Typ in
 *       Model/dashboard/DashboardSummary.ts definieren statt freiem string.
 */
type Props = {
  status: string;
};

export const StatusBadge = ({ status }: Props) => {
  return (
    <span className={`status-badge ${status}`}>
      {status === "offen" && "Ohne Rückmeldung"}
      {status === "antwort" && "Mit Rückmeldung"}
      {status === "unfertig" && "Nicht fertiggestellt"}
    </span>
  );
};