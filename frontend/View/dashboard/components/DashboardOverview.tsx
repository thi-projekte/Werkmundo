import { ArrowUpRight, ChevronRight, CircleAlert, FileText } from "lucide-react";
import { Link } from "react-router-dom";
import type { DashboardStatsResponse } from "../../../infrastructure/api/dashboardApi";
import { DashboardStats } from "./DashboardStats";

type Props = {
  data: DashboardStatsResponse | null;
  loading: boolean;
  error: Error | null;
  onShowAll?: () => void;
  quota?: {
    used: number;
    limit: number;
    planLabel: string;
    loading: boolean;
    error: boolean;
    nextPlan?: { id: string; label: string; offers: number };
  };
};

export const DashboardOverview = ({
  data,
  loading,
  error,
  onShowAll,
  quota,
}: Props) => (
  <>
    <section className="wm-overview-panel" aria-labelledby="wm-overview-title">
      <div className="wm-section-heading">
        <h2 id="wm-overview-title">Deine Angebote im Überblick</h2>
        <Link className="wm-overview-all" to="/dokumente?tab=angebote" onClick={onShowAll ? (event) => { event.preventDefault(); onShowAll(); } : undefined}>
          Alle Angebote <ArrowUpRight size={14} aria-hidden="true" />
        </Link>
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
          <Link className="wm-list-all-link" to="/dokumente" aria-label="Alle Dokumente anzeigen"><ArrowUpRight size={18} aria-hidden="true" /></Link>
        </div>
        {data && data.aufmerksamkeitErforderlich.length > 0 ? (
          <ul className="wm-dashboard-items">
            {data.aufmerksamkeitErforderlich.slice(0, 6).map((offer) => (
              <li key={offer.offerId}>
                <Link className="wm-dashboard-item-link" to={`/angebote/${encodeURIComponent(offer.businessKey)}/bearbeiten`}>
                  <span className="wm-list-icon"><CircleAlert size={15} /></span>
                  <span>
                    <strong>Angebot {offer.businessKey}</strong>
                    <small>Rückmeldung ausstehend</small>
                  </span>
                  <ChevronRight size={16} aria-hidden="true" />
                </Link>
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
          <Link className="wm-list-all-link" to="/dokumente" aria-label="Alle Dokumente anzeigen"><ArrowUpRight size={18} aria-hidden="true" /></Link>
        </div>
        {data && data.letzteAktivitaeten.length > 0 ? (
          <ul className="wm-dashboard-items">
            {data.letzteAktivitaeten.slice(0, 6).map((activity) => (
              <li key={`${activity.offerId}-${activity.zeitpunkt}`}>
                <Link className="wm-dashboard-item-link" to={`/angebote/${encodeURIComponent(activity.businessKey)}/bearbeiten`}>
                  <span className="wm-list-icon"><FileText size={15} /></span>
                  <span>
                    <strong>Angebot {activity.businessKey}</strong>
                    <small>
                      {new Date(activity.zeitpunkt).toLocaleDateString("de-DE")}
                    </small>
                  </span>
                  <ChevronRight size={16} aria-hidden="true" />
                </Link>
              </li>
            ))}
          </ul>
        ) : error ? (
          <p className="wm-dashboard-feedback">Aktivitäten konnten nicht geladen werden.</p>
        ) : !loading ? (
          <p className="wm-dashboard-feedback">Noch keine Aktivitäten vorhanden.</p>
        ) : (
          <p className="wm-dashboard-feedback">Aktivitäten werden geladen …</p>
        )}
      </section>
    </div>

    {quota && (
      <section className="wm-quota-panel" aria-labelledby="wm-quota-title">
        <div className="wm-quota-heading">
          <h2 id="wm-quota-title">Dein Angebotskontingent</h2>
          {!quota.loading && !quota.error && (
            <strong>{Math.max(0, quota.limit - quota.used)} Angebote verfügbar</strong>
          )}
        </div>
        {quota.error ? (
          <p className="wm-dashboard-feedback">Dein Angebotskontingent konnte nicht geladen werden.</p>
        ) : quota.loading ? (
          <p className="wm-dashboard-feedback">Dein Angebotskontingent wird geladen …</p>
        ) : (
          <>
            <div
              className="wm-quota-progress"
              role="progressbar"
              aria-label={`Monatliches Angebotskontingent im Tarif ${quota.planLabel}`}
              aria-valuemin={0}
              aria-valuemax={quota.limit}
              aria-valuenow={Math.min(quota.used, quota.limit)}
            >
              <span style={{ width: `${Math.min(100, quota.used / quota.limit * 100)}%` }} />
            </div>
            <div className="wm-quota-footer">
              <span>{quota.used} von {quota.limit} Angeboten verbraucht · Tarif {quota.planLabel}</span>
              {quota.used >= quota.limit && (
                <div className="wm-quota-limit-notice" role="status">
                  <span>Dein Kontingent ist ausgeschöpft. Du kannst auf einen größeren Tarif wechseln.</span>
                  {quota.nextPlan ? (
                    <Link to="/profil?tab=abo">Tarif {quota.nextPlan.label} ansehen</Link>
                  ) : (
                    <a href="mailto:kontakt@werkmundo.de">Enterprise anfragen</a>
                  )}
                </div>
              )}
            </div>
          </>
        )}
      </section>
    )}
  </>
);
