package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.application.port.WorkflowNotifier;
import de.winfprojekt.werkmundo.aiservice.domain.AnalyseAuftrag;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Use-Case "Spracheingabe analysieren": LLM-Call 1 → LLM-Call 2 → Ergebnis an die Process Engine.
 *
 * <p><b>Warum asynchron?</b> Der HTTP-Connector der Process Engine blockiert die Prozess-
 * ausfuehrung waehrend unseres Aufrufs. Erst nach unserer Antwort legt die Engine am folgenden
 * Receive Task die Subscription fuer die Message {@code ergebnisKI} an. Deshalb antwortet die
 * api-Schicht sofort mit 202 und die eigentliche Arbeit laeuft hier im Hintergrund
 * ({@link ManagedExecutor}, damit der Quarkus-Kontext erhalten bleibt). Fehler nach der 202
 * koennen nur noch geloggt werden.
 */
@ApplicationScoped
public class AnalyzeVoiceInputUseCase {

    private static final Logger LOG = Logger.getLogger(AnalyzeVoiceInputUseCase.class);

    private final LlmCall1Generator call1;
    private final Call2Selector call2;
    private final WorkflowNotifier workflowNotifier;
    private final ManagedExecutor executor;

    public AnalyzeVoiceInputUseCase(LlmCall1Generator call1,
                                    Call2Selector call2,
                                    WorkflowNotifier workflowNotifier,
                                    ManagedExecutor executor) {
        this.call1 = call1;
        this.call2 = call2;
        this.workflowNotifier = workflowNotifier;
        this.executor = executor;
    }

    /** Startet die Verarbeitung im Hintergrund und kehrt sofort zurueck. */
    public void startAsync(AnalyseAuftrag auftrag) {
        LOG.infof("Starte KI-Verarbeitung %s (businessKey=%s)", auftrag.typ(), auftrag.businessKey());
        executor.runAsync(() -> analysiereUndMelde(auftrag))
                .exceptionally(t -> {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    // Kurze Meldung (z.B. "Process Engine nicht erreichbar"), Stacktrace nur auf DEBUG.
                    LOG.errorf("KI-Verarbeitung fehlgeschlagen (businessKey=%s): %s",
                            auftrag.businessKey(), cause.getMessage());
                    LOG.debug("Details", cause);
                    return null;
                });
    }

    /** Fuehrt Call 1 und Call 2 synchron aus und liefert das Ergebnis (ohne Process Engine). */
    public ErgebnisKi analysiere(AnalyseAuftrag auftrag) {
        ErgebnisKi ergebnis = call1.generate(auftrag);
        return call2.enrich(ergebnis, auftrag.handwerkerId());
    }

    void analysiereUndMelde(AnalyseAuftrag auftrag) {
        ErgebnisKi ergebnis = analysiere(auftrag);
        workflowNotifier.sendeErgebnis(auftrag.businessKey(), ergebnis);
    }
}
