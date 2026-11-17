package de.werkmundo.document.infrastructure;

import de.werkmundo.document.application.port.DocumentSender;
import de.werkmundo.document.domain.Document;
import de.werkmundo.document.domain.DocumentType;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MailDocumentSender implements DocumentSender {
    private static final String APPLICATION_PDF = "application/pdf";

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "document.mail.from")
    String from;

    @Override
    public void sendToCustomer(Document document, String accessLink) {
        String subject = switch (document.type()) {
            case OFFER -> "Ihr Angebot";
            case INVOICE -> "Ihre Rechnung";
            case ORDER_CONFIRMATION -> "Ihre Auftragsbestätigung";
        };
        String body = emailBody(document.type(), accessLink);

        mailer.send(Mail.withText(document.recipientEmail(), subject, body)
                .setFrom(from)
                .addAttachment(document.fileName(), document.pdfContent(), APPLICATION_PDF));
    }

    private static String emailBody(DocumentType type, String accessLink) {
        String documentName = switch (type) {
            case OFFER -> "Ihr Angebot";
            case INVOICE -> "Ihre Rechnung";
            case ORDER_CONFIRMATION -> "Ihre Auftragsbestätigung";
        };
        String body = "Guten Tag,\n\nanbei erhalten Sie " + documentName + " als PDF.\n";
        if (type == DocumentType.OFFER) {
            body += "\nSie können Ihr Angebot auch über diesen Link aufrufen:\n" + accessLink + "\n";
        }
        return body + "\nFreundliche Grüße";
    }
}
