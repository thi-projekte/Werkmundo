/*
 * [Squad P] KOPIERT AUS: frontend/src/features/dashboard/components/DashboardCard.tsx
 * NEUE SCHICHT: View (reine Darstellungskomponente) - kein Handlungsbedarf.
 * Optional: Da generisch, ggf. nach Beispiel/frontend/shared/components/ verschieben.
 */
type Props = {
  title: string;
  value: string;
};

export const DashboardCard = ({ title, value }: Props) => {
  return (
    <div className="dashboard-card">
      <p className="card-title">{title}</p>
      <h2>{value}</h2>
    </div>
  );
};