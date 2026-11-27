import { DashboardCard } from "./DashboardCard";
import type { DashboardStatsResponse } from "../../../infrastructure/api/dashboardApi";
import { Check, CircleDashed, FileX2, Send } from "lucide-react";

type Props = {
  data: DashboardStatsResponse;
};

export const DashboardStats = ({ data }: Props) => {
  return (
    <div className="stats-grid wm-offer-stats">
      <DashboardCard
        title="Entwürfe"
        value={data.nichtFertiggestellt.toString()}
        icon={<CircleDashed size={18} />}
      />
      <DashboardCard
        title="Versendet · offen"
        value={data.ohneRueckmeldung.toString()}
        icon={<Send size={18} />}
      />
      <DashboardCard
        title="Angenommen"
        value={data.angenommen.toString()}
        icon={<Check size={18} />}
      />
      <DashboardCard
        title="Abgelehnt"
        value={data.abgelehnt.toString()}
        icon={<FileX2 size={18} />}
      />
    </div>
  );
};
