package de.winfprojekt.werkmundo.aiservice.api;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

/** Endpoint-Test ohne DEEPGRAM_API_KEY → Stub-Transkript. */
@QuarkusTest
class TranscriptionResourceTest {

    @Test
    void transcribe_liefert_transkript() {
        given().multiPart("audio", "aufnahme.webm", new byte[]{1, 2, 3, 4}, "audio/webm")
                .when().post("/ai/transcribe")
                .then().statusCode(200)
                .body("transkript", containsString("Wallbox"));
    }

    @Test
    void transcribe_ohne_audio_400() {
        given().multiPart("anderes", "x")
                .when().post("/ai/transcribe")
                .then().statusCode(400)
                .body("error", notNullValue());
    }

    @Test
    void cors_preflight_fuer_frontend() {
        given().header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/ai/transcribe")
                .then().statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:5173");
    }
}
