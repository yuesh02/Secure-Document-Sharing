package com.secureprint.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Service;

@Service
public class WatermarkService {
    private static final DateTimeFormatter UTC = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    public byte[] apply(byte[] original, UUID transactionId, String shopName, Instant timestamp) {
        try (PDDocument document = Loader.loadPDF(original); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String footer = "Txn: " + transactionId + " | " + shopName + " | " + UTC.format(timestamp);
            for (PDPage page : document.getPages()) {
                PDRectangle crop = page.getCropBox();
                float width = crop.getWidth();
                float height = crop.getHeight();
                try (PDPageContentStream stream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                    stream.beginText();
                    stream.newLineAtOffset(crop.getLowerLeftX() + 18, crop.getLowerLeftY() + 12);
                    stream.showText(footer);
                    stream.endText();

                    PDExtendedGraphicsState state = new PDExtendedGraphicsState();
                    state.setNonStrokingAlphaConstant(0.12f);
                    stream.setGraphicsStateParameters(state);
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), Math.max(18, Math.min(width, height) / 9));
                    stream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45), crop.getLowerLeftX() + width / 4, crop.getLowerLeftY() + height / 2));
                    stream.showText(shopName);
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to watermark PDF", ex);
        }
    }
}
