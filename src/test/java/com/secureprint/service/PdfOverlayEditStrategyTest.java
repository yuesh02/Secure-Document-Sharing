package com.secureprint.service;

import com.secureprint.dto.OverlayRequest;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PdfOverlayEditStrategyTest {
    @Test void appliesOverlayWithoutChangingPageCount() throws Exception {
        byte[] source;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            source = output.toByteArray();
        }
        byte[] edited = new PdfOverlayEditStrategy().apply(source,
                List.of(new OverlayRequest(1, 50f, 50f, "Edited", 12f)));
        try (PDDocument document = Loader.loadPDF(edited)) {
            assertEquals(1, document.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(document).contains("Edited"));
        }
    }

    @Test void rejectsOverlayOutsidePageRange() throws Exception {
        byte[] source;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            source = output.toByteArray();
        }
        assertThrows(IllegalArgumentException.class, () -> new PdfOverlayEditStrategy().apply(source,
                List.of(new OverlayRequest(2, 1f, 1f, "bad", 12f))));
    }
}
