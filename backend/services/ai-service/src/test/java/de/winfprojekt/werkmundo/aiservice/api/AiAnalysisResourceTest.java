package de.winfprojekt.werkmundo.aiservice.api;

import de.winfprojekt.werkmundo.aiservice.infrastructure.processengine.CamundaCorrelationRequest;
import de.winfprojekt.werkmundo.aiservice.infrastructure.processengine.CamundaMessageClient;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Endpoint-Tests (ohne MEGALLM_API_KEY → Stub, Mock-Katalog, Process Engine gemockt).
 */
@QuarkusTest
class AiAnalysisResourceTest {

    @InjectMock
    @RestClient
    CamundaMessageClient camundaClient;

    @BeforeEach
    void setUp() {
        when(camundaClient.correlate(any())).thenAnswer(invocation -> Response.status(204).build());
    }

    static final String ERSTANGEBOT_PAYLOAD = """
            {
              "businessKey": "BK-T-ERST",
              "vorlage": {
                "leistungen": [ {"bezeichnung":"Wallbox montieren","beschreibung":"Montage","menge":1,"einheit":"Stk"} ],
                "material": [],
                "notizen": ["Vor Ort prüfen"]
              },
              "sprachschnipsel": "Beim Kunden eine Wallbox in der Garage installieren."
            }
            """;

    static final String KORREKTUR_PAYLOAD = """
            {
              "businessKey": "BK-T-KORR",
              "strukturierteAngebotspositionen": {
                "leistungen": [ {"bezeichnung":"Wallbox montieren","beschreibung":"Montage","menge":1,"einheit":"Stk"} ],
                "material": [],
                "notizen": []
              },
              "korrekturschnipsel": "Bitte noch einen FI-Schutzschalter ergänzen."
            }
            """;

    @Test
    void process_erstangebot_202_und_businessKey() {
        given().contentType("application/json").body(ERSTANGEBOT_PAYLOAD)
                .when().post("/ai/process")
                .then().statusCode(202)
                .body("status", equalTo("accepted"))
                .body("businessKey", equalTo("BK-T-ERST"));
    }

    @Test
    void process_korreliert_ergebnisKI_asynchron() {
        given().contentType("application/json").body(ERSTANGEBOT_PAYLOAD)
                .when().post("/ai/process")
                .then().statusCode(202);

        ArgumentCaptor<CamundaCorrelationRequest> captor = ArgumentCaptor.forClass(CamundaCorrelationRequest.class);
        verify(camundaClient, timeout(3000)).correlate(captor.capture());

        CamundaCorrelationRequest sent = captor.getValue();
        Assertions.assertEquals("ergebnisKI", sent.messageName());
        Assertions.assertEquals("BK-T-ERST", sent.businessKey());
        Assertions.assertTrue(sent.processVariables().get("ergebnisKI").value().contains("Wallbox"));
    }

    @Test
    void process_korrektur_wird_korreliert() {
        given().contentType("application/json").body(KORREKTUR_PAYLOAD)
                .when().post("/ai/process")
                .then().statusCode(202)
                .body("businessKey", equalTo("BK-T-KORR"));

        verify(camundaClient, timeout(3000)).correlate(any());
    }

    @Test
    void process_nicht_zuordenbar_400_ohne_korrelation() {
        given().contentType("application/json").body("{ \"businessKey\": \"BK-T-LEER\" }")
                .when().post("/ai/process")
                .then().statusCode(400)
                .body("error", notNullValue());

        verify(camundaClient, after(500).never()).correlate(any());
    }

    @Test
    void process_leerer_body_400() {
        given().contentType("application/json").body("{}")
                .when().post("/ai/process")
                .then().statusCode(400);

        verify(camundaClient, never()).correlate(any());
    }

    @Test
    void analyze_liefert_ergebnis_synchron_mit_katalog_ids() {
        given().contentType("application/json").body(ERSTANGEBOT_PAYLOAD)
                .when().post("/ai/analyze")
                .then().statusCode(200)
                .body("strukturierteAngebotspositionen.material", hasSize(4))
                .body("strukturierteAngebotspositionen.material[0].bezeichnung", equalTo("Wallbox"))
                // "Starkstromkabel NYM-J 5x6mm²" findet im Mock-Katalog die NYM-J-Leitung (id 3004)
                .body("strukturierteAngebotspositionen.material[1].katalogProduktId", equalTo("3004"))
                .body("geschaetzteArbeitsdauerStunden", equalTo(4.0f));

        verify(camundaClient, never()).correlate(any());
    }

    @Test
    void health_ist_up() {
        given().when().get("/q/health/ready").then().statusCode(200);
    }
}
