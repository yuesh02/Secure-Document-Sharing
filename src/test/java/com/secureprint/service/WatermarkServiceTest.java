package com.secureprint.service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WatermarkServiceTest {
    @Test void watermarkAppearsOnEveryPage() throws Exception {
        byte[] source;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.save(output);
            source = output.toByteArray();
        }
        UUID transaction = UUID.randomUUID();
        byte[] watermarked = new WatermarkService().apply(source, transaction, "rahul", Instant.parse("2026-01-01T00:00:00Z"));
        try (PDDocument document = Loader.loadPDF(watermarked)) {
            String text = new PDFTextStripper().getText(document);
            assertEquals(2, text.split("Txn: " + transaction).length - 1);
            assertTrue(text.contains("rahul"));
        }
    }
}
