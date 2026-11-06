package de.winfprojekt.werkmundo.aiservice.domain;

/**
 * Ein Analyseauftrag an die KI - unabhaengig davon, wie er per HTTP angekommen ist.
 *
 * <p>Wird ausschliesslich ueber {@link #of} erzeugt. Dort steckt die Fachregel, welcher
 * {@link ProcessType} vorliegt (gleiche Reihenfolge wie das BPMN-Script: Korrektur vor
 * Erstangebot):
 * <ul>
 *   <li><b>Korrektur:</b> bestehende Positionen UND {@code korrekturschnipsel} vorhanden</li>
 *   <li><b>Erstangebot:</b> {@code vorlage} UND {@code sprachschnipsel} vorhanden</li>
 *   <li><b>sonst:</b> {@link UngueltigerAuftragException} (fail-fast, z.B. auch fuer die nicht
 *       unterstuetzte Rechnungskorrektur, deren Payload Preise enthaelt)</li>
 * </ul>
 *
 * <p>Kundendaten sind bewusst nicht enthalten - sie gehen nicht an die KI.
 *
 * @param businessKey          Korrelations-ID der Prozessinstanz
 * @param handwerkerId         Keycloak-ID des Handwerkers (Mandant fuer die Katalogsuche), optional
 * @param typ                  abgeleiteter Pipeline-Typ
 * @param vorlage              Vorlage (nur Erstangebot)
 * @param sprachschnipsel      transkribierter Sprachschnipsel (nur Erstangebot)
 * @param bestehendePositionen zu ueberarbeitende Positionen (nur Korrektur)
 * @param korrekturschnipsel   transkribierte Korrektur (nur Korrektur)
 */
public record AnalyseAuftrag(
        String businessKey,
        String handwerkerId,
        ProcessType typ,
        Vorlage vorlage,
        String sprachschnipsel,
        Angebotspositionen bestehendePositionen,
        String korrekturschnipsel
) {

    public static AnalyseAuftrag of(String businessKey,
                                    String handwerkerId,
                                    Vorlage vorlage,
                                    String sprachschnipsel,
                                    Angebotspositionen bestehendePositionen,
                                    String korrekturschnipsel) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new UngueltigerAuftragException("businessKey fehlt - ohne ihn kann das Ergebnis "
                    + "keiner Prozessinstanz zugeordnet werden.");
        }
        if (bestehendePositionen != null && korrekturschnipsel != null) {
            return new AnalyseAuftrag(businessKey, handwerkerId, ProcessType.KORREKTUR,
                    null, null, bestehendePositionen, korrekturschnipsel);
        }
        if (vorlage != null && sprachschnipsel != null) {
            return new AnalyseAuftrag(businessKey, handwerkerId, ProcessType.ERSTANGEBOT,
                    vorlage, sprachschnipsel, null, null);
        }
        throw new UngueltigerAuftragException(
                "Payload enthaelt weder Erstangebot-Felder (vorlage + sprachschnipsel) noch "
                        + "Korrektur-Felder (strukturierteAngebotspositionen + korrekturschnipsel). "
                        + "Rechnungskorrektur wird nicht unterstuetzt. businessKey=" + businessKey);
    }
}
