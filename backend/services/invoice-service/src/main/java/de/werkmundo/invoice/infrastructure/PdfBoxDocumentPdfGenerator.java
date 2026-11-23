package de.werkmundo.invoice.infrastructure;

import de.werkmundo.invoice.document.application.port.DocumentPdfGenerator;
import de.werkmundo.invoice.document.domain.DocumentSnapshot;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.LineItem;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.Party;
import de.werkmundo.invoice.document.domain.DocumentSnapshot.TaxLine;
import de.werkmundo.invoice.document.domain.DocumentType;
import jakarta.enterprise.context.ApplicationScoped;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

@ApplicationScoped
public class PdfBoxDocumentPdfGenerator implements DocumentPdfGenerator {
    private static final float LEFT_MARGIN = 50;
    private static final float TOP_MARGIN = 790;
    private static final float BOTTOM_MARGIN = 50;
    private static final float LINE_HEIGHT = 16;
    private static final float MAX_LINE_WIDTH = 495;
    private static final int MAX_LINES_PER_PAGE =
            (int) ((TOP_MARGIN - BOTTOM_MARGIN) / LINE_HEIGHT) + 1;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter OFFER_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final float OFFER_MARGIN = 48;
    private static final float OFFER_WIDTH = 499;
    private static final float OFFER_FOOTER_LIMIT = 120;
    private static final Color BRAND_PURPLE = new Color(41, 23, 65);
    private static final Color BRAND_GREEN = new Color(146, 210, 82);
    private static final Color LIGHT_PURPLE = new Color(247, 245, 250);
    private static final Color LIGHT_GREEN = new Color(243, 249, 236);
    private static final Color BORDER_COLOR = new Color(224, 221, 230);
    private static final Color BODY_COLOR = new Color(48, 45, 54);
    private static final Color MUTED_COLOR = new Color(105, 101, 111);
    private static final Color WARNING_COLOR = new Color(160, 45, 48);

    @Override
    public byte[] generate(DocumentType type, DocumentSnapshot snapshot) {
        if (type == DocumentType.OFFER) {
            return generateOffer(snapshot);
        }

        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        List<Line> lines = createLines(type, snapshot, regular, bold);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int start = 0; start < lines.size(); start += MAX_LINES_PER_PAGE) {
                int end = Math.min(start + MAX_LINES_PER_PAGE, lines.size());
                writePage(document, lines.subList(start, end));
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate document PDF", exception);
        }
    }

    private static byte[] generateOffer(DocumentSnapshot snapshot) {
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                InputStream logoStream = PdfBoxDocumentPdfGenerator.class.getResourceAsStream("/werkmundo-logo.png")) {
            if (logoStream == null) {
                throw new IllegalStateException("Werkmundo logo resource is missing");
            }
            PDImageXObject logo =
                    PDImageXObject.createFromByteArray(document, logoStream.readAllBytes(), "werkmundo-logo");
            OfferPage page = new OfferPage(document, snapshot, logo, regular, bold, true);
            try {
                int itemIndex = 0;
                for (LineItem item : snapshot.items()) {
                    float rowHeight = page.itemHeight(item);
                    if (page.cursorY - rowHeight < OFFER_FOOTER_LIMIT + 20) {
                        page.drawFooter();
                        page.close();
                        page = new OfferPage(document, snapshot, logo, regular, bold, false);
                    }
                    page.drawItem(item, snapshot.taxLines(), itemIndex++);
                }

                if (page.cursorY - page.summaryHeight(snapshot) < OFFER_FOOTER_LIMIT + 20) {
                    page.drawFooter();
                    page.close();
                    page = new OfferPage(document, snapshot, logo, regular, bold, false);
                }
                page.drawSummary(snapshot);
                page.drawFooter();
            } finally {
                page.close();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate offer PDF", exception);
        }
    }

    private static void drawText(
            PDPageContentStream content,
            String text,
            PDFont font,
            int size,
            Color color,
            float x,
            float y) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private static void drawRightText(
            PDPageContentStream content,
            String text,
            PDFont font,
            int size,
            Color color,
            float right,
            float y) throws IOException {
        float width = font.getStringWidth(text) / 1000 * size;
        drawText(content, text, font, size, color, right - width, y);
    }

    private static void drawCenteredText(
            PDPageContentStream content,
            String text,
            PDFont font,
            int size,
            Color color,
            float left,
            float width,
            float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        drawText(content, text, font, size, color, left + (width - textWidth) / 2, y);
    }

    private static List<String> wrapText(String text, PDFont font, int size, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.replaceAll("[\\r\\n\\t]+", " ").trim();
        while (!remaining.isEmpty()) {
            int end = remaining.length();
            while (end > 1 && font.getStringWidth(remaining.substring(0, end)) / 1000 * size > width) {
                int space = remaining.lastIndexOf(' ', end - 1);
                end = space > 0 ? space : end - 1;
            }
            lines.add(remaining.substring(0, end).trim());
            remaining = remaining.substring(end).trim();
        }
        return lines;
    }

    private static String offerMoney(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.GERMANY);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value) + " €";
    }

    private static String quantityText(BigDecimal quantity, String unit) {
        String value = quantity.stripTrailingZeros().toPlainString();
        return unit == null || unit.isBlank() ? value : value + " " + unit;
    }

    private static String itemTaxText(LineItem item, List<TaxLine> taxLines) {
        if (taxLines.isEmpty()) {
            return "—";
        }
        TaxLine tax = taxLines.get(0);
        if (tax.rate() == null) {
            return offerMoney(BigDecimal.ZERO);
        }
        BigDecimal amount = item.lineTotal()
                .multiply(tax.rate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return offerMoney(amount) + " (" + tax.rate().stripTrailingZeros().toPlainString() + "%)";
    }

    private static final class OfferPage implements AutoCloseable {
        private static final float[] COLUMN_WIDTHS = {220, 55, 75, 78, 71};
        private static final float ROW_TEXT_SIZE = 9;
        private static final float ROW_LINE_HEIGHT = 12;

        private final PDPageContentStream content;
        private final PDFont regular;
        private final PDFont bold;
        private float cursorY;
        private boolean closed;

        OfferPage(
                PDDocument document,
                DocumentSnapshot snapshot,
                PDImageXObject logo,
                PDFont regular,
                PDFont bold,
                boolean firstPage) throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            this.regular = regular;
            this.bold = bold;

            float pageWidth = page.getMediaBox().getWidth();
            if (firstPage) {
                float logoWidth = 180;
                float logoHeight = logoWidth * logo.getHeight() / logo.getWidth();
                content.drawImage(logo, OFFER_MARGIN, 750, logoWidth, logoHeight);
                drawRightText(
                        content,
                        "Angebot # " + valueOrDash(snapshot.referenceNumber()),
                        bold,
                        9,
                        BRAND_PURPLE,
                        pageWidth - OFFER_MARGIN,
                        770);
                drawText(content, "ABSENDER", bold, 9, BRAND_PURPLE, OFFER_MARGIN, 726);
                drawText(content, "EMPFÄNGER", bold, 9, BRAND_PURPLE, 320, 726);

                float customerY = drawParagraph(
                        content, snapshot.customer().name(), bold, 10, BODY_COLOR, 320, 707, 227, 13);
                customerY = drawParagraph(
                        content, snapshot.customer().address(), regular, 9, BODY_COLOR, 320, customerY - 2, 227, 12);
                drawParagraph(
                        content, snapshot.customer().email(), regular, 9, MUTED_COLOR, 320, customerY - 2, 227, 12);
                if (snapshot.issuer() != null) {
                    Party issuer = snapshot.issuer();
                    float issuerY = 707;
                    if (issuer.name() != null && !issuer.name().isBlank()) {
                        drawText(content, issuer.name(), bold, 10, BODY_COLOR, OFFER_MARGIN, issuerY);
                        issuerY -= 14;
                    }
                    issuerY = drawParagraph(
                                    content,
                                    issuer.address(),
                                    regular,
                                    9,
                                    BODY_COLOR,
                                    OFFER_MARGIN,
                                    issuerY,
                                    250,
                                    12)
                            - 2;
                    drawParagraph(
                            content,
                            issuer.email(),
                            regular,
                            9,
                            MUTED_COLOR,
                            OFFER_MARGIN,
                            issuerY,
                            250,
                            12);
                }
                drawText(content, "DATUM", bold, 8, BRAND_GREEN, 320, 650);
                drawText(
                        content,
                        OFFER_DATE_FORMAT.format(snapshot.documentDate()),
                        regular,
                        10,
                        BODY_COLOR,
                        320,
                        635);
                cursorY = 623;
            } else {
                float logoWidth = 145;
                float logoHeight = logoWidth * logo.getHeight() / logo.getWidth();
                content.drawImage(logo, OFFER_MARGIN, 755, logoWidth, logoHeight);
                drawRightText(
                        content,
                        "Angebot # " + valueOrDash(snapshot.referenceNumber()) + " · Fortsetzung",
                        bold,
                        8,
                        BRAND_PURPLE,
                        pageWidth - OFFER_MARGIN,
                        770);
                cursorY = 716;
            }
            drawTableHeader();
        }

        private static String valueOrDash(String value) {
            return value == null || value.isBlank() ? "—" : value;
        }

        private static float drawParagraph(
                PDPageContentStream content,
                String text,
                PDFont font,
                int size,
                Color color,
                float x,
                float y,
                float width,
                float lineHeight) throws IOException {
            List<String> lines = wrapText(text, font, size, width);
            for (String line : lines) {
                drawText(content, line, font, size, color, x, y);
                y -= lineHeight;
            }
            return y;
        }

        private void drawTableHeader() throws IOException {
            float x = OFFER_MARGIN;
            float height = 28;
            float bottom = cursorY - height;
            content.setNonStrokingColor(BRAND_PURPLE);
            content.addRect(OFFER_MARGIN, bottom, OFFER_WIDTH, height);
            content.fill();

            String[] labels = {"BESCHREIBUNG", "MENGE", "PREIS", "GESAMT", "UST."};
            for (int i = 0; i < labels.length; i++) {
                if (i == 0) {
                    drawText(content, labels[i], bold, 8, Color.WHITE, x + 8, bottom + 10);
                } else {
                    drawCenteredText(content, labels[i], bold, 8, Color.WHITE, x, COLUMN_WIDTHS[i], bottom + 10);
                }
                x += COLUMN_WIDTHS[i];
            }
            content.setNonStrokingColor(BRAND_GREEN);
            content.addRect(OFFER_MARGIN, bottom - 2, OFFER_WIDTH, 2);
            content.fill();
            cursorY = bottom - 5;
        }

        private float itemHeight(LineItem item) throws IOException {
            int lineCount = Math.max(1, wrapText(item.description(), regular, (int) ROW_TEXT_SIZE, COLUMN_WIDTHS[0] - 16).size());
            return Math.max(30, lineCount * ROW_LINE_HEIGHT + 12);
        }

        private void drawItem(LineItem item, List<TaxLine> taxLines, int index) throws IOException {
            List<String> description = wrapText(
                    item.description(), regular, (int) ROW_TEXT_SIZE, COLUMN_WIDTHS[0] - 16);
            float height = itemHeight(item);
            float bottom = cursorY - height + 5;
            content.setNonStrokingColor(index % 2 == 0 ? Color.WHITE : LIGHT_PURPLE);
            content.addRect(OFFER_MARGIN, bottom, OFFER_WIDTH, height);
            content.fill();

            float textY = cursorY - 8;
            for (String line : description) {
                drawText(content, line, regular, (int) ROW_TEXT_SIZE, BODY_COLOR, OFFER_MARGIN + 8, textY);
                textY -= ROW_LINE_HEIGHT;
            }

            float x = OFFER_MARGIN + COLUMN_WIDTHS[0];
            drawCenteredText(
                    content,
                    quantityText(item.quantity(), item.unit()),
                    regular,
                    8,
                    BODY_COLOR,
                    x,
                    COLUMN_WIDTHS[1],
                    cursorY - 16);
            x += COLUMN_WIDTHS[1];
            drawRightText(content, offerMoney(item.unitPrice()), regular, 8, BODY_COLOR, x + COLUMN_WIDTHS[2] - 6, cursorY - 16);
            x += COLUMN_WIDTHS[2];
            drawRightText(content, offerMoney(item.lineTotal()), regular, 8, BODY_COLOR, x + COLUMN_WIDTHS[3] - 6, cursorY - 16);
            x += COLUMN_WIDTHS[3];
            drawRightText(content, itemTaxText(item, taxLines), regular, 7, BODY_COLOR, x + COLUMN_WIDTHS[4] - 6, cursorY - 16);

            content.setStrokingColor(BORDER_COLOR);
            content.setLineWidth(0.5f);
            content.moveTo(OFFER_MARGIN, bottom);
            content.lineTo(OFFER_MARGIN + OFFER_WIDTH, bottom);
            content.stroke();
            cursorY -= height;
        }

        private float summaryHeight(DocumentSnapshot snapshot) throws IOException {
            float height = (snapshot.taxLines().size() + 2) * 20 + 20;
            if (snapshot.notes() != null && !snapshot.notes().isBlank()) {
                height += Math.max(1, wrapText(snapshot.notes(), regular, 8, OFFER_WIDTH).size()) * 12 + 8;
            }
            return height;
        }

        private void drawSummary(DocumentSnapshot snapshot) throws IOException {
            float x = OFFER_MARGIN + 250;
            float width = OFFER_WIDTH - 250;
            float y = cursorY - 10;

            drawRightText(content, "GESAMT", regular, 9, BODY_COLOR, x + 135, y);
            drawRightText(content, offerMoney(snapshot.netAmount()), regular, 9, BODY_COLOR, OFFER_MARGIN + OFFER_WIDTH, y);
            y -= 20;
            for (TaxLine tax : snapshot.taxLines()) {
                String label = tax.rate() == null ? tax.label() : tax.label() + " (" + tax.rate().stripTrailingZeros().toPlainString() + "%)";
                drawRightText(content, label, regular, 9, BODY_COLOR, x + 135, y);
                drawRightText(content, offerMoney(tax.amount()), regular, 9, BODY_COLOR, OFFER_MARGIN + OFFER_WIDTH, y);
                y -= 20;
            }

            content.setNonStrokingColor(LIGHT_GREEN);
            content.addRect(x, y - 6, width, 23);
            content.fill();
            content.setStrokingColor(BRAND_GREEN);
            content.setLineWidth(0.8f);
            content.moveTo(x, y + 17);
            content.lineTo(x + width, y + 17);
            content.stroke();
            drawRightText(content, "Gesamtpreis", bold, 10, BRAND_PURPLE, x + 135, y + 2);
            drawRightText(content, offerMoney(snapshot.totalAmount()), bold, 10, BRAND_PURPLE, OFFER_MARGIN + OFFER_WIDTH, y + 2);
            cursorY = y - 20;

            if (snapshot.notes() != null && !snapshot.notes().isBlank()) {
                drawParagraph(
                        content,
                        "Hinweis: " + snapshot.notes(),
                        regular,
                        8,
                        snapshot.items().isEmpty() ? WARNING_COLOR : MUTED_COLOR,
                        OFFER_MARGIN,
                        cursorY,
                        OFFER_WIDTH,
                        12);
            }
        }

        private void drawFooter() throws IOException {
            content.setStrokingColor(BORDER_COLOR);
            content.setLineWidth(0.6f);
            content.moveTo(OFFER_MARGIN, 99);
            content.lineTo(OFFER_MARGIN + OFFER_WIDTH, 99);
            content.stroke();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                content.close();
                closed = true;
            }
        }
    }

    private static List<Line> createLines(
            DocumentType type, DocumentSnapshot snapshot, PDFont regular, PDFont bold) {
        List<Line> lines = new ArrayList<>();
        String title = switch (type) {
            case OFFER -> "Offer";
            case INVOICE -> "Invoice";
        };
        addWrapped(lines, title, bold, 20);
        addWrapped(lines, "Date: " + DATE_FORMAT.format(snapshot.documentDate()), bold, 11);
        if (type == DocumentType.INVOICE) {
            addWrapped(lines, "Invoice number (placeholder): " + snapshot.invoiceNumber(), bold, 11);
        }
        if (snapshot.issuer() != null) {
            addWrapped(lines, "", regular, 10);
            addWrapped(lines, "From", bold, 12);
            addParty(lines, snapshot.issuer(), regular);
        }
        addWrapped(lines, "", regular, 10);
        addWrapped(lines, "Customer", bold, 12);
        addParty(lines, snapshot.customer(), regular);
        addWrapped(lines, "", regular, 10);
        addWrapped(lines, "Items", bold, 12);
        for (LineItem item : snapshot.items()) {
            String unit = item.unit() == null || item.unit().isBlank() ? "" : " " + item.unit();
            addWrapped(lines,
                    item.description() + " | " + item.quantity().toPlainString() + unit
                            + " x " + money(item.unitPrice(), snapshot.currency())
                            + " = " + money(item.lineTotal(), snapshot.currency()),
                    regular, 10);
        }
        addWrapped(lines, "", regular, 10);
        addWrapped(lines, "Net amount: " + money(snapshot.netAmount(), snapshot.currency()), regular, 10);
        for (TaxLine taxLine : snapshot.taxLines()) {
            String rate = taxLine.rate() == null ? "" : " (" + taxLine.rate().toPlainString() + "%)";
            addWrapped(lines, taxLine.label() + rate + ": " + money(taxLine.amount(), snapshot.currency()),
                    regular, 10);
        }
        addWrapped(lines, "Total: " + money(snapshot.totalAmount(), snapshot.currency()), bold, 12);
        if (snapshot.notes() != null && !snapshot.notes().isBlank()) {
            addWrapped(lines, "", regular, 10);
            addWrapped(lines, "Notes: " + snapshot.notes(), regular, 10);
        }
        return lines;
    }

    private static void addParty(List<Line> lines, Party party, PDFont regular) {
        if (party.name() != null && !party.name().isBlank()) {
            addWrapped(lines, party.name(), regular, 10);
        }
        if (party.address() != null && !party.address().isBlank()) {
            addWrapped(lines, party.address(), regular, 10);
        }
        if (party.email() != null && !party.email().isBlank()) {
            addWrapped(lines, party.email(), regular, 10);
        }
    }

    private static void addWrapped(List<Line> lines, String text, PDFont font, int fontSize) {
        if (text.isEmpty()) {
            lines.add(new Line("", font, fontSize));
            return;
        }
        String remaining = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        while (!remaining.isEmpty()) {
            int breakAt = remaining.length();
            while (breakAt > 1 && textWidth(font, fontSize, remaining.substring(0, breakAt)) > MAX_LINE_WIDTH) {
                int spaceAt = remaining.lastIndexOf(' ', breakAt - 1);
                breakAt = spaceAt > 0 ? spaceAt : breakAt - 1;
            }
            lines.add(new Line(remaining.substring(0, breakAt).trim(), font, fontSize));
            remaining = remaining.substring(breakAt).trim();
        }
    }

    private static float textWidth(PDFont font, int fontSize, String text) {
        try {
            return font.getStringWidth(text) / 1000 * fontSize;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Text contains characters that cannot be written to a PDF", exception);
        }
    }

    private static void writePage(PDDocument document, List<Line> lines) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float y = TOP_MARGIN;
            for (Line line : lines) {
                if (y < BOTTOM_MARGIN) {
                    break;
                }
                content.beginText();
                content.setFont(line.font(), line.fontSize());
                content.newLineAtOffset(LEFT_MARGIN, y);
                content.showText(line.text());
                content.endText();
                y -= LINE_HEIGHT;
            }
        }
    }

    private static String money(BigDecimal amount, String currency) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " " + currency;
    }

    private record Line(String text, PDFont font, int fontSize) {
    }
}