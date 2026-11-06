package de.winfprojekt.werkmundo.aiservice.infrastructure.processengine;

/**
 * Message- und Variablennamen, die mit Squad Prozesse (BPMN) abgestimmt sein muessen.
 * Dokumentiert in {@code contracts/workflow/ErgebnisKiMessage.json}.
 */
public final class WorkflowMessageNames {

    /** Name der BPMN-Message am Receive Task nach dem KI-Aufruf. */
    public static final String ERGEBNIS_KI_MESSAGE = "ergebnisKI";

    /** Name der Prozessvariable, in die das Ergebnis (JSON als String) geschrieben wird. */
    public static final String ERGEBNIS_KI_VARIABLE = "ergebnisKI";

    private WorkflowMessageNames() {
    }
}
