/*
 * =====================================================================================
 * [Squad P] KOPIERT AUS: frontend/src/features/Laden/components/LadenPage.tsx
 * NEUE SCHICHT: View  (Wartebildschirm nach der Spracheingabe, bis das KI-Ergebnis da ist)
 * -------------------------------------------------------------------------------------
 * WAS JETZT ZU TUN IST:
 *
 * 1. Neues ViewModel anlegen: Beispiel/frontend/ViewModel/voice/useAiLoadingViewModel.ts
 *    Dorthin verschieben:
 *      - Auslesen von location.state (LadenState)
 *      - pollingStarted-Ref und der komplette Polling-useEffect (pollUntilKiFertig,
 *        Navigation nach /review bzw. /angebotTeilen bzw. zurück nach /home)
 *    Rückgabe z.B.: { mode } - mehr braucht die View nicht.
 *
 * 2. interface LadenState -> Beispiel/frontend/Model/voice/ (z.B. AiLoadingState.ts) oder
 *    ins ViewModel; Platzhalter Model/voice/VoiceRecording.ts ist dafür NICHT gedacht.
 *
 * 3. Theme-Erkennung (isLight + MutationObserver) -> gemeinsamer Hook
 *    Beispiel/frontend/shared/hooks/useTheme.ts (siehe ProfilPage.tsx Punkt 2).
 *
 * 4. OFFEN / mit Squad J klären: pollUntilKiFertig() kommt aus dem offer-service
 *    ("@/data/api/offerService", Squad J). Der Modus "versand-warten" (Weiterleitung zu
 *    /angebotTeilen) gehört fachlich zum Angebotsversand = Squad J.
 *    -> Klären, ob diese Seite gemeinsam genutzt wird (dann evtl. shared/components/) oder
 *       Squad J eine eigene Warteseite für den Versand bekommt.
 *
 * 5. Imports anpassen (bewusst noch NICHT geändert):
 *      "./LadenPage.css"          -> bleibt (selber Ordner)
 *      "@/assets/logos/..."       -> Assets nicht kopiert (frontend/src/assets)
 *      "@/data/api/offerService"  -> Squad J, nicht kopiert (später infrastructure/api/offerApi.ts)
 * =====================================================================================
 */
import { useEffect, useRef, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { pollUntilKiFertig } from "@/data/api/offerService";
import logoWhite from "@/assets/logos/CraftVoice_Logo_white.png";
import logoBlack from "@/assets/logos/CraftVoice_Logo_black.png";
import "./LadenPage.css";

interface LadenState {
  businessKey?: string;
  offerId?: number;
  mode?: "ki-warten" | "versand-warten";
  nextRoute?: string; // Ziel-Route nach Polling (default: /review)
  // Bei einer KI-Korrektur (Fall 3) der Server-Zeitstempel des Angebots VOR dem
  // Re-Run. Das Polling wartet dann auf ein echt neueres KI-Ergebnis, statt
  // sofort das bereits vorhandene "KI_FERTIG" zurückzugeben.
  sinceUpdatedAt?: string;
}

export const LadenPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state ?? {}) as LadenState;

  const {
    businessKey,
    offerId,
    mode = "ki-warten",
    nextRoute,
    sinceUpdatedAt,
  } = state;

  // Verhindert doppeltes Starten des Pollings bei StrictMode-Remounts
  const pollingStarted = useRef(false);

  // Logo an Dark-/Light-Mode anpassen (reagiert live auf Theme-Wechsel,
  // gleiches Muster wie Navbar/AppHeader).
  const [isLight, setIsLight] = useState(
    () => document.documentElement.getAttribute("data-theme") === "light",
  );

  useEffect(() => {
    const observer = new MutationObserver(() => {
      setIsLight(
        document.documentElement.getAttribute("data-theme") === "light",
      );
    });
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["data-theme"],
    });
    return () => observer.disconnect();
  }, []);

  const logo = isLight ? logoBlack : logoWhite;

  useEffect(() => {
    // Kein businessKey → Fallback zur HomeView
    if (!businessKey) {
      console.warn(
        "[LadenPage] Kein businessKey im State — Weiterleitung zu /home",
      );
      setTimeout(() => navigate("/home"), 2000);
      return;
    }

    if (pollingStarted.current) return;
    pollingStarted.current = true;

    if (mode === "ki-warten") {
      // Polling auf offer-service bis Status KI_FERTIG
      pollUntilKiFertig(
        businessKey,
        2500, // alle 2,5 Sekunden
        300_000, // max. 5 Minuten
        (status) => {
          console.log(`[LadenPage] Angebot ${businessKey}: Status = ${status}`);
        },
        // Bei Fall 3 gesetzt: erst ein NACH diesem Zeitstempel aktualisiertes
        // Angebot gilt als fertig (sonst würde das alte KI_FERTIG sofort matchen).
        sinceUpdatedAt,
      )
        .then((offer) => {
          // KI-Ergebnis bereit → zur ReviewPage mit allen nötigen Infos
          navigate(nextRoute ?? "/review", {
            state: {
              businessKey: offer.businessKey,
              offerId: offer.id,
            },
          });
        })
        .catch((err) => {
          console.error("[LadenPage] Polling fehlgeschlagen:", err);
          // Bei Timeout oder Fehler: zurück zur HomeView mit Fehlermeldung
          navigate("/home", {
            state: {
              error:
                "Das KI-Ergebnis ist nicht rechtzeitig angekommen. Bitte erneut versuchen.",
            },
          });
        });
    } else if (mode === "versand-warten") {
      // Kurze Wartezeit für den Versandprozess, dann zur OfferSharePage
      setTimeout(() => {
        navigate(nextRoute ?? "/angebotTeilen", {
          state: { businessKey, offerId },
        });
      }, 3000);
    }
  }, [businessKey, mode, navigate, nextRoute, offerId, sinceUpdatedAt]);

  return (
    <div className="ai-loading-page">
      <div className="ai-loading-container">
        <div className="logo-wrapper">
          <img src={logo} alt="CraftVoice Logo" className="logo-image" />
        </div>
        <br />
        <h1>
          {mode === "versand-warten"
            ? "Angebot wird vorbereitet"
            : "KI analysiert deine Aufnahme"}
        </h1>
        <br />
        <p>
          Bitte warte einen Moment.
          <br />
          <br />
          {mode === "versand-warten"
            ? "Das Angebot-PDF wird erstellt."
            : "Die Ergebnisse werden verarbeitet und vorbereitet."}
        </p>
        <br />
        <br />
        <div className="loader">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  );
};
