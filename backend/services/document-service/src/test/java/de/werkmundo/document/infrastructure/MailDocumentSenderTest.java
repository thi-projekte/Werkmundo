package de.werkmundo.document.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentType;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MailDocumentSenderTest {
    private static final byte[] PDF = {37, 80, 68, 70};

    @Test
    void sendsInvoiceAndOrderConfirmationAsPdfWithoutLink() {
        assertSentWithoutLink(
                new Document(DocumentType.INVOICE, "invoice-1", "owner-1", "customer-1",
                        "customer@example.com", "invoice.pdf", PDF),
                "Ihre Rechnung");
        assertSentWithoutLink(
                new Document(DocumentType.ORDER_CONFIRMATION, "order-1", "owner-1", "customer-1",
                        "customer@example.com", "order-confirmation.pdf", PDF),
                "Ihre Auftragsbestätigung");
    }

    @Test
    void includesOfferLinkInMailBodyAndAttachesPdf() {
        String link = "https://example.test/offer?token=opaque-token";
        Mail mail = send(new Document(DocumentType.OFFER, "offer-1", "owner-1", "customer-1",
                "customer@example.com", "offer.pdf", PDF), link);

        assertEquals("Ihr Angebot", mail.getSubject());
        assertTrue(mail.getText().contains(link));
        assertAttachment(mail, "offer.pdf");
    }

    private static void assertSentWithoutLink(Document document, String subject) {
        Mail mail = send(document, null);

        assertEquals(subject, mail.getSubject());
        assertFalse(mail.getText().contains("http"));
        assertAttachment(mail, document.fileName());
    }

    private static Mail send(Document document, String accessLink) {
        Mailer mailer = mock(Mailer.class);
        MailDocumentSender sender = new MailDocumentSender();
        sender.mailer = mailer;
        sender.from = "noreply@example.test";

        sender.sendToCustomer(document, accessLink);

        ArgumentCaptor<Mail> captor = ArgumentCaptor.forClass(Mail.class);
        verify(mailer).send(captor.capture());
        Mail mail = captor.getValue();
        assertEquals(List.of("customer@example.com"), mail.getTo());
        assertEquals("noreply@example.test", mail.getFrom());
        return mail;
    }

    private static void assertAttachment(Mail mail, String fileName) {
        assertEquals(1, mail.getAttachments().size());
        assertEquals(fileName, mail.getAttachments().getFirst().getName());
        assertEquals("application/pdf", mail.getAttachments().getFirst().getContentType());
    }
}
