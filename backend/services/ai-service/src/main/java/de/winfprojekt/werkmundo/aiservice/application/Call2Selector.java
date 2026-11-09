package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.application.port.CatalogSearchPort;
import de.winfprojekt.werkmundo.aiservice.application.port.LlmPort;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;
import de.winfprojekt.werkmundo.aiservice.domain.Position;
import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM-Call 2 (Produktauswahl): waehlt fuer jede <b>Materialposition</b> ein Katalogprodukt und
 * setzt dessen ID als {@code katalogProduktId}. Leistungspositionen bleiben unveraendert.
 *
 * <p>Pro Position: Kandidaten suchen ({@link CatalogSearchPort}, ohne Preise) → LLM waehlt genau
 * einen oder {@code KEIN_TREFFER}. Die Positionen laufen parallel ({@link ManagedExecutor}),
 * weil grosse Angebote 40-60 Positionen haben koennen.
 *
 * <p><b>Baseline-Fallback:</b> Ohne LLM oder bei einem LLM-Fehler wird der Top-Kandidat der
 * Suche genommen. Dann geht die "kein Treffer"-Faehigkeit verloren - die kann nur der LLM-Pfad.
 */
@ApplicationScoped
public class Call2Selector {

    private static final Logger LOG = Logger.getLogger(Call2Selector.class);
    static final int CANDIDATE_LIMIT = 15;

    private final LlmPort llm;
    private final CatalogSearchPort catalogSearch;
    private final Call2PromptBuilder promptBuilder;
    private final Call2AnswerParser answerParser;
    private final ManagedExecutor executor;

    public Call2Selector(LlmPort llm,
                         CatalogSearchPort catalogSearch,
                         Call2PromptBuilder promptBuilder,
                         Call2AnswerParser answerParser,
                         ManagedExecutor executor) {
        this.llm = llm;
        this.catalogSearch = catalogSearch;
        this.promptBuilder = promptBuilder;
        this.answerParser = answerParser;
        this.executor = executor;
    }

    /**
     * Reichert die Materialpositionen um Katalog-IDs an (parallel).
     *
     * @param handwerkerId Keycloak-ID des Handwerkers, dessen Katalog durchsucht wird (im Mock irrelevant)
     */
    public ErgebnisKi enrich(ErgebnisKi ergebnis, String handwerkerId) {
        if (ergebnis == null || ergebnis.strukturierteAngebotspositionen() == null) {
            return ergebnis;
        }
        List<Position> material = ergebnis.strukturierteAngebotspositionen().material();
        if (material.isEmpty()) {
            return ergebnis;
        }

        List<CompletableFuture<Position>> futures = new ArrayList<>(material.size());
        for (Position p : material) {
            futures.add(executor.supplyAsync(() -> selectFor(p, handwerkerId)));
        }
        List<Position> enriched = futures.stream().map(CompletableFuture::join).toList();

        return ergebnis.withPositionen(ergebnis.strukturierteAngebotspositionen().withMaterial(enriched));
    }

    /** Waehlt fuer eine einzelne Materialposition das Katalogprodukt. */
    Position selectFor(Position position, String handwerkerId) {
        // Suche nur mit der bezeichnung: die Katalogsuche ist token-restriktiv, eine lange
        // beschreibung drueckt die Trefferzahl auf 0. Die beschreibung bekommt das LLM als Kontext.
        String query = position.bezeichnung() == null ? "" : position.bezeichnung().trim();

        List<ProduktKandidat> candidates = catalogSearch.search(query, CANDIDATE_LIMIT, handwerkerId);
        if (candidates.isEmpty()) {
            LOG.debugf("Call 2: keine Kandidaten fuer '%s'.", position.bezeichnung());
            return position;
        }

        if (!llm.isConfigured()) {
            ProduktKandidat top = candidates.get(0);
            LOG.debugf("Call 2: kein LLM -> Baseline (Top-Kandidat) fuer '%s': %s.",
                    position.bezeichnung(), top.articleNumber());
            return position.withKatalogProduktId(top.id());
        }

        try {
            String raw = llm.complete(promptBuilder.systemPrompt(), promptBuilder.userContent(position, candidates));
            String pick = answerParser.parsePick(raw);

            if (Call2AnswerParser.KEIN_TREFFER.equals(pick)) {
                LOG.debugf("Call 2: LLM -> KEIN_TREFFER fuer '%s'.", position.bezeichnung());
                return position;
            }

            ProduktKandidat chosen = candidates.stream()
                    .filter(c -> pick.equalsIgnoreCase(c.articleNumber()))
                    .findFirst()
                    .orElse(null);
            if (chosen == null) {
                LOG.warnf("Call 2: LLM-Auswahl '%s' ist nicht in der Kandidatenliste fuer '%s'.",
                        pick, position.bezeichnung());
                return position;
            }

            LOG.infof("Call 2: '%s' -> %s (%s).", position.bezeichnung(), chosen.articleNumber(), chosen.name());
            return position.withKatalogProduktId(chosen.id());

        } catch (RuntimeException e) {
            ProduktKandidat top = candidates.get(0);
            LOG.errorf(e, "Call 2 fehlgeschlagen fuer '%s' -> Baseline Top-Kandidat %s.",
                    position.bezeichnung(), top.articleNumber());
            return position.withKatalogProduktId(top.id());
        }
    }
}
