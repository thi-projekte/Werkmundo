import type { ReactNode } from "react";

type Props = {
  title: string;
  value: string;
  icon?: ReactNode;
};

export const DashboardCard = ({ title, value, icon }: Props) => {
  return (
    <div className="dashboard-card">
      <h2>{value}</h2>
      <p className="card-title">{title}</p>
      {icon && <span className="wm-stat-icon" aria-hidden="true">{icon}</span>}
    </div>
  );
};