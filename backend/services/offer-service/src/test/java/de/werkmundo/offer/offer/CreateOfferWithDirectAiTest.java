package de.werkmundo.offer.offer;

import de.werkmundo.offer.api.H2StartupTestProfile;
import de.werkmundo.offer.ai.AiAnalyzeResult;
import de.werkmundo.offer.ai.AiDirectClient;
import de.werkmundo.offer.processengine.ProcessEngineClient;
import de.werkmundo.offer.user.StundensatzResponse;
import de.werkmundo.offer.user.UserServiceClient;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * "Angebot erstellen" im Direktmodus: der ai-service wird ohne Process Engine aufgerufen.
 */
@QuarkusTest
@TestProfile(H2StartupTestProfile.class)
@TestSecurity(user = "direct-ai-owner", roles = "OWNER")
@OidcSecurity(claims = {
        @Claim(key = "sub", value = "direct-ai-owner")
})
class CreateOfferWithDirectAiTest {

    private static final String OWNER = "direct-ai-owner";

    @InjectMock
    AiDirectClient aiDirectClient;

    @InjectMock
    ProcessEngineClient processEngineClient;

    @InjectMock
    @RestClient
    UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        reset(aiDirectClient, processEngineClient, userServiceClient);
        when(aiDirectClient.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        // Einzeln löschen, damit Positionen und Statushistorie per Cascade mitgehen
        QuarkusTransaction.requiringNew().run(() ->
                Offer.<Offer>list("handwerkerId", OWNER).forEach(Offer::delete));
    }

    @Test
    void uebernimmtKiPositionenAlsBearbeitbarenEntwurf() {
        StundensatzResponse stundensatz = new StundensatzResponse();
        stundensatz.stundensatz = new BigDecimal("50.00");
        when(userServiceClient.getStundensatz()).thenReturn(stundensatz);

        AiAnalyzeResult ergebnis = new AiAnalyzeResult(
                new AiAnalyzeResult.Positionen(
                        List.of(new AiAnalyzeResult.Position("Wallbox montieren", null, 1.0, "pauschal", null)),
                        List.of(new AiAnalyzeResult.Position("Wallbox 11 kW", "mit Kabel", 1.0, "Stk", "1001")),
                        List.of()),
                List.of("Absicherung prüfen"),
                2.5);
        when(aiDirectClient.analysiere(anyString(), eq(OWNER), eq("Wallbox in der Garage montieren")))
                .thenReturn(Optional.of(ergebnis));

        given()
                .contentType("application/json")
                .body("""
                        {"customerId": "7", "speechSnippet": "Wallbox in der Garage montieren", "title": "Wallbox"}
                        """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .body("status", equalTo(Offer.STATUS_ENTWURF))
                .body("positions", hasSize(3))
                .body("positions[0].bezeichnung", equalTo("Wallbox montieren"))
                .body("positions[0].type", equalTo("OTHER"))
                .body("positions[1].bezeichnung", equalTo("Wallbox 11 kW"))
                .body("positions[1].katalogProduktId", equalTo(null))
                .body("positions[2].bezeichnung", equalTo("Arbeitszeit"))
                .body("positions[2].positionsPreis", equalTo(125.0f))
                .body("gesamtPreis", equalTo(125.0f));

        verifyNoInteractions(processEngineClient);
    }

    @Test
    void legtLeerenEntwurfAnWennKiNichtAntwortet() {
        when(aiDirectClient.analysiere(anyString(), anyString(), anyString())).thenReturn(Optional.empty());

        given()
                .contentType("application/json")
                .body("""
                        {"customerId": "7", "speechSnippet": "Irgendwas"}
                        """)
                .when()
                .post("/offers")
                .then()
                .statusCode(201)
                .body("status", equalTo(Offer.STATUS_ENTWURF))
                .body("positions", hasSize(0));

        verify(processEngineClient, never()).sendAngebotPayload(any(), any(), any(), any(), any());
    }
}
