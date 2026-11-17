package de.werkmundo.document.infrastructure;

import de.werkmundo.document.api.dto.DocumentSnapshotRequest;
import de.werkmundo.document.application.port.DocumentPdfGenerator;
import de.werkmundo.document.domain.DocumentType;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Override
    public byte[] generate(DocumentType type, DocumentSnapshotRequest snapshot) {
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

    private static List<Line> createLines(
            DocumentType type, DocumentSnapshotRequest snapshot, PDFont regular, PDFont bold) {
        List<Line> lines = new ArrayList<>();
        String title = switch (type) {
            case OFFER -> "Offer";
            case INVOICE -> "Invoice";
            case ORDER_CONFIRMATION -> "Order confirmation";
        };
        addWrapped(lines, title, bold, 20);
        addWrapped(lines, "Date: " + DATE_FORMAT.format(snapshot.documentDate()), bold, 11);
        addWrapped(lines, "", regular, 10);
        addWrapped(lines, "From", bold, 12);
        addParty(lines, snapshot.issuer(), regular);
        addWrapped(lines, "", regular, 10);
        addWrapped(lines, "Customer", bold, 12);
        addParty(lines, snapshot.customer(), regular);
        addWrapped(lines, "", regular, 10);
        addWrapped(lines, "Items", bold, 12);
        for (DocumentSnapshotRequest.LineItem item : snapshot.items()) {
            String unit = item.unit() == null || item.unit().isBlank() ? "" : " " + item.unit();
            addWrapped(lines,
                    item.description() + " | " + item.quantity().toPlainString() + unit
                            + " x " + money(item.unitPrice(), snapshot.currency())
                            + " = " + money(item.lineTotal(), snapshot.currency()),
                    regular, 10);
        }
        addWrapped(lines, "", regular, 10);
        addWrapped(lines, "Net amount: " + money(snapshot.netAmount(), snapshot.currency()), regular, 10);
        for (DocumentSnapshotRequest.TaxLine taxLine : snapshot.taxLines()) {
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

    private static void addParty(List<Line> lines, DocumentSnapshotRequest.Party party, PDFont regular) {
        addWrapped(lines, party.name(), regular, 10);
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

    private static String money(java.math.BigDecimal amount, String currency) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " " + currency;
    }

    private record Line(String text, PDFont font, int fontSize) {
    }
}
