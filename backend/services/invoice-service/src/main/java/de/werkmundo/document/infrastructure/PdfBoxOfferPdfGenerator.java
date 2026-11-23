package de.werkmundo.document.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import de.werkmundo.document.application.port.OfferPdfGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

@ApplicationScoped
public class PdfBoxOfferPdfGenerator implements OfferPdfGenerator {
    private static final int LINES_PER_PAGE = 35;
    private static final float LINE_HEIGHT = 18;

    @Override
    public byte[] generate(JsonNode offer, boolean demo) {
        if (offer == null || !offer.isObject()) {
            throw new IllegalArgumentException("Offer data must be an object");
        }

        List<String> lines = offerLines(offer, demo);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    writeText(content, bold, 21, "WERKMUNDO", 52, 785);
                    writeText(content, bold, 15, "ANGEBOT", 52, 755);

                    float y = 720;
                    int end = Math.min(start + LINES_PER_PAGE, lines.size());
                    for (int index = start; index < end; index++) {
                        boolean heading = index == 0;
                        writeText(content, heading ? bold : regular, heading ? 12 : 10,
                                lines.get(index), 52, y);
                        y -= heading ? 23 : LINE_HEIGHT;
                    }

                    if (demo) {
                        addDemoWatermark(content, bold);
                        writeText(content, bold, 8,
                                "DEMO - Nicht zur geschaeftlichen Verwendung bestimmt.",
                                52, 38);
                    }
                }
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Offer PDF could not be generated", exception);
        }
    }

    private static List<String> offerLines(JsonNode offer, boolean demo) {
        List<String> lines = new ArrayList<>();
        String title = value(offer.path("title"), value(offer.path("businessKey"), "Angebot"));
        lines.add(title);
        lines.add("Angebotsnummer: " + value(offer.path("businessKey"), "—"));
        lines.add(" ");
        lines.add("Anbieter");
        JsonNode seller = offer.path("seller");
        addWrapped(lines, value(seller.path("companyName"), ""));
        addWrapped(lines, value(seller.path("contact"), ""));
        addWrapped(lines, value(seller.path("street"), ""));
        addWrapped(lines, value(seller.path("postalCode"), "") + " "
                + value(seller.path("city"), ""));
        addWrapped(lines, value(seller.path("email"), ""));
        lines.add(" ");
        lines.add("Empfaenger");

        JsonNode customer = offer.path("customer");
        String customerName = value(customer.path("name"), "");
        if (demo && customerName.isBlank()) {
            customerName = "Musterkunde - Demo-Vorschau";
        }
        addWrapped(lines, customerName.isBlank() ? "Nicht angegeben" : customerName);
        addWrapped(lines, value(customer.path("street"), ""));
        addWrapped(lines, value(customer.path("postalCode"), "") + " "
                + value(customer.path("city"), ""));
        addWrapped(lines, value(customer.path("email"), ""));
        lines.add(" ");
        lines.add("Leistungen");

        JsonNode positions = offer.path("positions");
        boolean hasMissingPrice = false;
        if (positions.isArray()) {
            for (JsonNode position : positions) {
                String description = value(position.path("bezeichnung"), "Beschreibung fehlt");
                String quantity = value(position.path("menge"), "");
                String unit = value(position.path("einheit"), "");
                String quantityText = quantity.isBlank() ? "Menge offen" : quantity;
                String unitText = unit.isBlank() ? "Einheit offen" : unit;
                addWrapped(lines, quantityText + " " + unitText + " - " + description);
                String price = value(position.path("einzelPreis"), "");
                if (demo) {
                    lines.add(!validPrice(price)
                            ? "Preis offen"
                            : "Beispielpreis (unverbindlich): " + price + " EUR");
                } else if (price.isBlank()) {
                    lines.add("Preis: —");
                    hasMissingPrice = true;
                } else {
                    lines.add("Einzelpreis netto: " + price + " EUR");
                }
                String details = value(position.path("beschreibung"), "");
                if (!details.isBlank()) addWrapped(lines, details);
            }
        }

        if (positions.isEmpty()) {
            lines.add("Noch keine KI-generierten Positionen vorhanden.");
        }

        if (!demo && !hasMissingPrice && positions.isArray()) {
            BigDecimal total = BigDecimal.ZERO;
            for (JsonNode position : positions) {
                BigDecimal quantity = decimal(position.path("menge"));
                BigDecimal price = decimal(position.path("einzelPreis"));
                total = total.add(quantity.multiply(price));
            }
            lines.add(" ");
            lines.add("Gesamt netto: " + NumberFormat.getCurrencyInstance(Locale.GERMANY).format(total));
        }

        if (demo) {
            lines.add(" ");
            lines.add("Alle Preise sind unverbindliche Beispielwerte. Dieses Dokument ist nicht zur "
                    + "geschaeftlichen Verwendung bestimmt.");
        }
        return lines;
    }

    private static void addWrapped(List<String> lines, String text) {
        if (text == null || text.isBlank()) return;
        String remaining = text.trim();
        while (remaining.length() > 92) {
            int breakAt = remaining.lastIndexOf(' ', 92);
            if (breakAt <= 0) breakAt = 92;
            lines.add(remaining.substring(0, breakAt));
            remaining = remaining.substring(breakAt).trim();
        }
        if (!remaining.isBlank()) lines.add(remaining);
    }

    private static void addDemoWatermark(PDPageContentStream content, PDType1Font bold)
            throws IOException {
        PDExtendedGraphicsState transparency = new PDExtendedGraphicsState();
        transparency.setNonStrokingAlphaConstant(0.16f);
        content.saveGraphicsState();
        content.setGraphicsStateParameters(transparency);
        content.beginText();
        content.setFont(bold, 82);
        content.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(35), 80, 320));
        content.showText("DEMO");
        content.endText();
        content.restoreGraphicsState();
    }

    private static void writeText(
            PDPageContentStream content,
            PDType1Font font,
            float fontSize,
            String text,
            float x,
            float y) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private static String value(JsonNode node, String fallback) {
        return node == null || node.isMissingNode() || node.isNull()
                ? fallback
                : node.asText(fallback);
    }

    private static BigDecimal decimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean validPrice(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return new BigDecimal(value).signum() >= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
