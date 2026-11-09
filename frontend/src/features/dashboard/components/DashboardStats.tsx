import { DashboardCard } from "@/features/dashboard/components/DashboardCard";
import type { DashboardStatsResponse } from "@/data/api/dashboardApi";
import { CheckCheck, CircleDashed, FileText, Send } from "lucide-react";

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
        title="Mit Rückmeldung"
        value={data.mitRueckmeldung.toString()}
        icon={<CheckCheck size={18} />}
      />
      <DashboardCard
        title="Angebote gesamt"
        value={data.angeboteGesamt.toString()}
        icon={<FileText size={18} />}
      />
    </div>
  );
};