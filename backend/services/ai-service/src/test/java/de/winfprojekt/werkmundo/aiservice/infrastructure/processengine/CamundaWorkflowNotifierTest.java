package de.winfprojekt.werkmundo.aiservice.infrastructure.processengine;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.winfprojekt.werkmundo.aiservice.application.StubResultGenerator;
import de.winfprojekt.werkmundo.aiservice.domain.ErgebnisKi;
import de.winfprojekt.werkmundo.aiservice.domain.ProcessType;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CamundaWorkflowNotifierTest {

    private final ErgebnisKi ergebnis = new StubResultGenerator().generate(ProcessType.ERSTANGEBOT);

    private static Response status(int code) {
        return Response.status(code).build();
    }

    @Test
    void erfolg_beimErstenVersuch() {
        CamundaMessageClient client = Mockito.mock(CamundaMessageClient.class);
        when(client.correlate(any())).thenReturn(status(204));

        new CamundaWorkflowNotifier(client, new ObjectMapper(), 5, 1).sendeErgebnis("BK-1", ergebnis);

        verify(client, times(1)).correlate(any());
    }

    @Test
    void http400_wirdWiederholt_bisSubscriptionDaIst() {
        CamundaMessageClient client = Mockito.mock(CamundaMessageClient.class);
        when(client.correlate(any())).thenReturn(status(400), status(400), status(204));

        new CamundaWorkflowNotifier(client, new ObjectMapper(), 5, 1).sendeErgebnis("BK-1", ergebnis);

        verify(client, times(3)).correlate(any());
    }

    @Test
    void http400_nachAllenVersuchen_wirft() {
        CamundaMessageClient client = Mockito.mock(CamundaMessageClient.class);
        when(client.correlate(any())).thenAnswer(inv -> status(400));

        assertThrows(IllegalStateException.class,
                () -> new CamundaWorkflowNotifier(client, new ObjectMapper(), 3, 1).sendeErgebnis("BK-1", ergebnis));
        verify(client, times(3)).correlate(any());
    }

    @Test
    void http500_wirftSofort() {
        CamundaMessageClient client = Mockito.mock(CamundaMessageClient.class);
        when(client.correlate(any())).thenReturn(status(500));

        assertThrows(IllegalStateException.class,
                () -> new CamundaWorkflowNotifier(client, new ObjectMapper(), 5, 1).sendeErgebnis("BK-1", ergebnis));
        verify(client, times(1)).correlate(any());
    }

    @Test
    void engineNichtErreichbar_wirftVerstaendlicheMeldung() {
        CamundaMessageClient client = Mockito.mock(CamundaMessageClient.class);
        when(client.correlate(any())).thenThrow(new ProcessingException("Connection refused"));

        assertThrows(IllegalStateException.class,
                () -> new CamundaWorkflowNotifier(client, new ObjectMapper(), 5, 1).sendeErgebnis("BK-1", ergebnis));
    }
}
