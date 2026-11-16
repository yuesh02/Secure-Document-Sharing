package com.secureprint.config;

import com.secureprint.entity.Role;
import com.secureprint.entity.User;
import com.secureprint.entity.UserStatus;
import com.secureprint.repository.UserRepository;
import com.secureprint.service.DocumentService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("demo")
public class DemoSeeder implements org.springframework.boot.CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final DocumentService documents;
    private final Clock clock;

    public DemoSeeder(UserRepository users, PasswordEncoder encoder, DocumentService documents, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.documents = documents;
        this.clock = clock;
    }

    @Override
    public void run(String... args) throws Exception {
        User owner = users.findByEmailIgnoreCase("owner@demo.local").orElseGet(() ->
                users.save(new User("Demo Owner", "owner@demo.local", encoder.encode("demo-owner-password"),
                        Role.USER, UserStatus.ACTIVE, Instant.now(clock))));
        users.findByEmailIgnoreCase("shop@demo.local").orElseGet(() ->
                users.save(new User("Demo Print Shop", "shop@demo.local", encoder.encode("demo-shop-password"),
                        Role.PRINT_SHOP, UserStatus.ACTIVE, Instant.now(clock))));
        if (documents.list(owner.getId(), 0, 1).totalElements() == 0) {
            documents.upload(owner.getId(), samplePdf());
        }
    }

    private MultipartFile samplePdf() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 18);
                content.newLineAtOffset(72, 720);
                content.showText("Secure Print demo document");
                content.endText();
            }
            pdf.save(output);
        }
        byte[] bytes = output.toByteArray();
        return new ByteArrayMultipartFile(bytes);
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private ByteArrayMultipartFile(byte[] bytes) { this.bytes = bytes; }
        public String getName() { return "file"; }
        public String getOriginalFilename() { return "demo.pdf"; }
        public String getContentType() { return "application/pdf"; }
        public boolean isEmpty() { return bytes.length == 0; }
        public long getSize() { return bytes.length; }
        public byte[] getBytes() { return bytes.clone(); }
        public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        public void transferTo(File destination) throws java.io.IOException { Files.write(destination.toPath(), bytes); }
    }
}
