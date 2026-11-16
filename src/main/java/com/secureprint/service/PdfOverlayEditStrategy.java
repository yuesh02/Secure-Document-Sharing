package com.secureprint.service;

import com.secureprint.dto.OverlayRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class PdfOverlayEditStrategy implements EditStrategy {
    @Override
    public byte[] apply(byte[] original, List<OverlayRequest> overlays) {
        try (PDDocument document = Loader.loadPDF(original); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (OverlayRequest overlay : overlays) {
                if (overlay.page() > document.getNumberOfPages()) {
                    throw new IllegalArgumentException("Overlay page is outside the document");
                }
                try (PDPageContentStream stream = new PDPageContentStream(document,
                        document.getPage(overlay.page() - 1), AppendMode.APPEND, true, true)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), overlay.fontSize());
                    stream.newLineAtOffset(overlay.x(), overlay.y());
                    stream.showText(overlay.text());
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unable to apply PDF overlays", ex);
        }
    }
}
