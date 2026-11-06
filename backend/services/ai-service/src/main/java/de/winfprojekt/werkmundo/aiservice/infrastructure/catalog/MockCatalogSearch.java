package de.winfprojekt.werkmundo.aiservice.infrastructure.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.application.port.CatalogSearchPort;
import de.winfprojekt.werkmundo.aiservice.domain.ProduktKandidat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Produktsuche gegen einen kleinen gebuendelten Mock-Katalog
 * ({@code src/main/resources/mock/catalog-mock.json}, 27 Artikel, ohne Preise).
 * Default fuer den lokalen Betrieb ({@code CATALOG_MOCK_ENABLED=true}), damit der ai-service
 * ohne catalog-service laeuft.
 *
 * <p>Ranking per einfachem Token-Overlap (Umlaute gefaltet). Das muss nur plausible Kandidaten
 * liefern; das echte Ranking macht der catalog-service.
 *
 * <p><b>Achtung:</b> Die Mock-IDs sind Zahlen, der echte Katalog vergibt UUIDs. Mit dem Mock
 * findet der offer-service daher keine Preise zu den IDs.
 */
@ApplicationScoped
@Typed(MockCatalogSearch.class)
public class MockCatalogSearch implements CatalogSearchPort {

    private static final Logger LOG = Logger.getLogger(MockCatalogSearch.class);
    static final String MOCK_RESOURCE = "mock/catalog-mock.json";

    private final List<ProduktKandidat> katalog;

    @Inject
    public MockCatalogSearch(ObjectMapper objectMapper) {
        this(ladeKatalog(objectMapper));
        LOG.infof("Katalogsuche im MOCK-Betrieb (%d Artikel).", katalog.size());
    }

    MockCatalogSearch(List<ProduktKandidat> katalog) {
        this.katalog = katalog == null ? List.of() : List.copyOf(katalog);
    }

    @Override
    public List<ProduktKandidat> search(String query, int limit, String handwerkerId) {
        Set<String> queryTokens = new HashSet<>(tokenize(query));
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        record Treffer(ProduktKandidat kandidat, long score) {}
        return katalog.stream()
                .map(k -> new Treffer(k, overlap(k, queryTokens)))
                .filter(t -> t.score() > 0)
                .sorted(Comparator.comparingLong(Treffer::score).reversed())
                .limit(limit)
                .map(Treffer::kandidat)
                .toList();
    }

    private static long overlap(ProduktKandidat k, Set<String> queryTokens) {
        Set<String> docTokens = new HashSet<>(tokenize(k.name() + " " + k.description() + " " + k.category()));
        return queryTokens.stream().filter(docTokens::contains).count();
    }

    private static List<String> tokenize(String text) {
        String folded = (text == null ? "" : text).toLowerCase()
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        List<String> tokens = new ArrayList<>();
        for (String t : folded.replaceAll("[^a-z0-9]+", " ").split("\\s+")) {
            if (t.length() >= 2) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    static List<ProduktKandidat> ladeKatalog(ObjectMapper objectMapper) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = MockCatalogSearch.class.getClassLoader();
        }
        try (InputStream is = cl.getResourceAsStream(MOCK_RESOURCE)) {
            if (is == null) {
                LOG.errorf("Mock-Katalog %s nicht gefunden - Mock-Suche liefert nichts.", MOCK_RESOURCE);
                return List.of();
            }
            JsonNode materials = objectMapper.readTree(is).get("materials");
            if (materials == null || !materials.isArray()) {
                LOG.errorf("Mock-Katalog %s hat kein 'materials'-Array.", MOCK_RESOURCE);
                return List.of();
            }
            List<ProduktKandidat> list = new ArrayList<>();
            for (JsonNode m : materials) {
                list.add(objectMapper.treeToValue(m, CatalogCandidateDto.class).toDomain());
            }
            return list;
        } catch (Exception e) {
            LOG.errorf(e, "Mock-Katalog %s konnte nicht geladen werden.", MOCK_RESOURCE);
            return List.of();
        }
    }
}
