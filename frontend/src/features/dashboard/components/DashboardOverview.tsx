import { ArrowUpRight, ChevronRight, CircleAlert, FileText } from "lucide-react";
import type { DashboardStatsResponse } from "@/data/api/dashboardApi";
import { DashboardStats } from "@/features/dashboard/components/DashboardStats";

type Props = {
  data: DashboardStatsResponse | null;
  loading: boolean;
  error: Error | null;
  onShowAll?: () => void;
};

export const DashboardOverview = ({
  data,
  loading,
  error,
  onShowAll,
}: Props) => (
  <>
    <section className="wm-overview-panel" aria-labelledby="wm-overview-title">
      <div className="wm-section-heading">
        <h2 id="wm-overview-title">Deine Angebote im Überblick</h2>
        {onShowAll && (
          <button type="button" onClick={onShowAll}>
            Alle Angebote <ArrowUpRight size={14} aria-hidden="true" />
          </button>
        )}
      </div>
      {loading && <p className="wm-dashboard-feedback">Angebote werden geladen …</p>}
      {error && (
        <p className="wm-dashboard-feedback wm-dashboard-error" role="alert">
          Angebote konnten nicht geladen werden: {error.message}
        </p>
      )}
      {data && <DashboardStats data={data} />}
    </section>

    <div className="wm-dashboard-lists">
      <section className="wm-list-panel" aria-labelledby="wm-attention-title">
        <div className="wm-section-heading">
          <h2 id="wm-attention-title">Das steht an</h2>
          <ArrowUpRight size={18} aria-hidden="true" />
        </div>
        {data && data.aufmerksamkeitErforderlich.length > 0 ? (
          <ul className="wm-dashboard-items">
            {data.aufmerksamkeitErforderlich.slice(0, 6).map((offer) => (
              <li key={offer.offerId}>
                <span className="wm-list-icon"><CircleAlert size={15} /></span>
                <span>
                  <strong>Angebot {offer.businessKey}</strong>
                  <small>Rückmeldung ausstehend</small>
                </span>
                <ChevronRight size={16} aria-hidden="true" />
              </li>
            ))}
          </ul>
        ) : (
          <p className="wm-dashboard-feedback">
            {loading
              ? "Deine Aufgaben werden geladen …"
              : error
                ? "Aufgaben konnten nicht geladen werden."
                : "Aktuell steht nichts an."}
          </p>
        )}
      </section>

      <section className="wm-list-panel" aria-labelledby="wm-recent-title">
        <div className="wm-section-heading">
          <h2 id="wm-recent-title">Zuletzt bearbeitet</h2>
          <ArrowUpRight size={18} aria-hidden="true" />
        </div>
        {data && data.letzteAktivitaeten.length > 0 ? (
          <ul className="wm-dashboard-items">
            {data.letzteAktivitaeten.slice(0, 6).map((activity) => (
              <li key={`${activity.offerId}-${activity.zeitpunkt}`}>
                <span className="wm-list-icon"><FileText size={15} /></span>
                <span>
                  <strong>Angebot {activity.businessKey}</strong>
                  <small>
                    {new Date(activity.zeitpunkt).toLocaleDateString("de-DE")}
                  </small>
                </span>
                <ChevronRight size={16} aria-hidden="true" />
              </li>
            ))}
          </ul>
        ) : !loading && !error ? (
          <p className="wm-dashboard-feedback">Noch keine Aktivitäten vorhanden.</p>
        ) : (
          <p className="wm-dashboard-feedback">Aktivitäten werden geladen …</p>
        )}
      </section>
    </div>
  </>
);
