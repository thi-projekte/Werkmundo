package de.winfprojekt.werkmundo.aiservice.application.port;

import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;

/** Meldet das KI-Ergebnis an die wartende Prozessinstanz der Process Engine. */
public interface WorkflowNotifier {

    /**
     * Sendet das Ergebnis an die Prozessinstanz mit diesem {@code businessKey}.
     *
     * @throws RuntimeException wenn die Engine das Ergebnis nicht annimmt
     */
    void sendeErgebnis(String businessKey, ErgebnisKi ergebnis);
}
