/*
 * [Squad P] KOPIERT AUS: frontend/src/features/dashboard/components/DashboardChart.tsx
 * NEUE SCHICHT: View (Teilkomponente von DashboardView)
 * TODO:
 *   1. Import "@/data/api/dashboardApi" -> Typ aus Model/dashboard/DashboardSummary.ts holen.
 *   2. Die Umwandlung in chartData (unten) ist Datenaufbereitung -> ins ViewModel
 *      (ViewModel/dashboard/useDashboard.ts), View bekommt fertige chartData als Prop.
 */
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { DashboardStatsResponse } from "@/data/api/dashboardApi";

type Props = {
  data: DashboardStatsResponse;
};

export const DashboardChart = ({ data }: Props) => {
  // Transform the data to match recharts format
  const chartData = data.angebotsuebersicht.map((item) => ({
    month: item.month,
    Angebote: item.angebote,
  }));

  return (
    <div className="dashboard-chart">
      <div className="chart-header">
        <h2>Angebotsübersicht</h2>
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />

          <XAxis dataKey="month" />

          <Tooltip />

          <Line
            type="monotone"
            dataKey="Angebote"
            stroke="#ff6a00"
            strokeWidth={3}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};