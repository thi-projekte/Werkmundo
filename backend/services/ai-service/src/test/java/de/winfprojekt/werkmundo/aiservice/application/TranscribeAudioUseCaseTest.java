package de.winfprojekt.werkmundo.aiservice.application;

import de.winfprojekt.werkmundo.aiservice.application.port.SpeechToTextPort;
import de.winfprojekt.werkmundo.aiservice.domain.LeeresTranskriptException;
import de.winfprojekt.werkmundo.aiservice.domain.UngueltigerAuftragException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranscribeAudioUseCaseTest {

    private static final byte[] AUDIO = {1, 2, 3};

    private static SpeechToTextPort port(boolean configured, String antwort) {
        return new SpeechToTextPort() {
            @Override
            public boolean isConfigured() {
                return configured;
            }

            @Override
            public String transcribe(byte[] audio, String contentType) {
                return antwort;
            }
        };
    }

    @Test
    void ohne_key_kommt_stub() {
        var useCase = new TranscribeAudioUseCase(port(false, null));
        assertEquals(TranscribeAudioUseCase.STUB_TRANSKRIPT, useCase.transkribiere(AUDIO, "audio/webm"));
    }

    @Test
    void mit_key_wird_transkript_getrimmt() {
        var useCase = new TranscribeAudioUseCase(port(true, "  Wallbox montieren  "));
        assertEquals("Wallbox montieren", useCase.transkribiere(AUDIO, "audio/webm;codecs=opus"));
    }

    @Test
    void leeres_transkript_wirft_422_exception() {
        var useCase = new TranscribeAudioUseCase(port(true, " "));
        assertThrows(LeeresTranskriptException.class, () -> useCase.transkribiere(AUDIO, "audio/webm"));
    }

    @Test
    void leere_aufnahme_wird_abgelehnt() {
        var useCase = new TranscribeAudioUseCase(port(true, "x"));
        assertThrows(UngueltigerAuftragException.class, () -> useCase.transkribiere(new byte[0], "audio/webm"));
    }

    @Test
    void kein_audio_typ_wird_abgelehnt() {
        var useCase = new TranscribeAudioUseCase(port(true, "x"));
        assertThrows(UngueltigerAuftragException.class, () -> useCase.transkribiere(AUDIO, "image/png"));
    }
}
