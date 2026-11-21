/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS: frontend/src/features/dashboard/hooks/useDashboard.ts
 * NEUE SCHICHT: ViewModel  (ersetzt den leeren Platzhalter useDashboardViewModel.ts)
 * -------------------------------------------------------------------------------------
 * WAS JETZT ZU TUN IST:
 *
 * 1. Filter-State und -Logik aus View/dashboard/DashboardView.tsx hierher holen
 *    (search, timeRange, isInTimeRange(), filteredData) und zusätzlich die chartData-
 *    Aufbereitung aus View/dashboard/DashboardChart.tsx.
 *
 * 2. Interface UseDashboardState -> Daten-Typ kommt aus
 *    Beispiel/frontend/Model/dashboard/DashboardSummary.ts (Platzhalter), siehe TODO in
 *    infrastructure/api/dashboardApi.ts.
 *
 * 3. Import anpassen (bewusst noch NICHT geändert):
 *      "@/data/api/dashboardApi" -> Beispiel/frontend/infrastructure/api/dashboardApi.ts
 *
 * 4. Optional: in useDashboardViewModel umbenennen (Namenskonvention der anderen ViewModels).
 * =====================================================================================
 */
import { useEffect, useState } from "react";
import { getDashboardStats, DashboardStatsResponse } from "@/data/api/dashboardApi";

interface UseDashboardState {
  data: DashboardStatsResponse | null;
  loading: boolean;
  error: Error | null;
}

export const useDashboard = () => {
  const [state, setState] = useState<UseDashboardState>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setState((prev) => ({ ...prev, loading: true, error: null }));
        const stats = await getDashboardStats();
        setState({ data: stats, loading: false, error: null });
      } catch (err) {
        setState({
          data: null,
          loading: false,
          error: err instanceof Error ? err : new Error("Unknown error"),
        });
      }
    };

    loadDashboard();
  }, []);

  return state;
};