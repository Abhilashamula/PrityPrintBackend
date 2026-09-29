package com.pingprint.document;

import com.pingprint.user.User;
import com.pingprint.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.ByteArrayOutputStream;

@Service
public class DocumentService {
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private final DocumentRepository documents;
    private final UserRepository users;
    private final Path storageRoot;

    public DocumentService(DocumentRepository documents, UserRepository users, @Value("${app.storage.documents-path:./data/documents}") String storagePath) {
        this.documents = documents; this.users = users; this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public Document store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("A document is required");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Document exceeds the 20 MB limit");
        String originalName = safeName(file.getOriginalFilename());
        String extension = extension(originalName).replace("jpeg", "jpg");
        if (!SetOfAllowed.EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Only PDF, JPG, and PNG files are supported");
        byte[] bytes = read(file);
        if (!matchesSignature(extension, bytes)) throw new IllegalArgumentException("The file contents do not match its extension");
        int pageCount = extension.equals("pdf") ? countPdfPages(bytes) : 1;
        User user = userId == null ? null : users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String storageKey = (userId == null ? "guest" : userId.toString()) + "/" + UUID.randomUUID() + "." + extension;
        try {
            Path target = storageRoot.resolve(storageKey).normalize();
            if (!target.startsWith(storageRoot)) throw new IllegalArgumentException("Invalid storage path");
            Files.createDirectories(target.getParent()); Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return documents.save(new Document(user, storageKey, originalName, contentType(extension), bytes.length, pageCount));
        } catch (IOException error) { throw new IllegalStateException("Could not store document", error); }
    }
    public byte[] read(Document document) {
        try {
            Path target = storageRoot.resolve(document.getStorageKey()).normalize();
            if (!target.startsWith(storageRoot)) throw new IllegalArgumentException("Invalid document storage path");
            return Files.readAllBytes(target);
        } catch (IOException error) { throw new IllegalStateException("Could not read stored document", error); }
    }
    public Document storeSystemTestPdf(String printerName) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                content.newLineAtOffset(72, 720); content.showText("Ping & Print test page");
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(0, -30); content.showText("Printer: " + printerName.replaceAll("[^A-Za-z0-9 ._-]", "_"));
                content.newLineAtOffset(0, -20); content.showText("Generated: " + Instant.now()); content.endText();
            }
            pdf.save(output);
            byte[] bytes = output.toByteArray();
            String storageKey = "system/" + UUID.randomUUID() + ".pdf";
            Path target = storageRoot.resolve(storageKey).normalize();
            if (!target.startsWith(storageRoot)) throw new IllegalArgumentException("Invalid storage path");
            Files.createDirectories(target.getParent()); Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return documents.save(new Document(null, storageKey, "ping-print-test.pdf", "application/pdf", bytes.length, 1));
        } catch (IOException error) { throw new IllegalStateException("Could not create test document", error); }
    }
    public void deleteStoredFile(Document document) {
        if (document.getDeletedAt() != null) return;
        try {
            Path target = storageRoot.resolve(document.getStorageKey()).normalize();
            if (!target.startsWith(storageRoot)) throw new IllegalArgumentException("Invalid document storage path");
            Files.deleteIfExists(target); document.markDeleted(); documents.save(document);
        } catch (IOException error) { throw new IllegalStateException("Could not delete stored document", error); }
    }

    private byte[] read(MultipartFile file) { try { return file.getBytes(); } catch (IOException error) { throw new IllegalArgumentException("Could not read document", error); } }
    private String safeName(String name) { String value = name == null ? "document" : Paths.get(name).getFileName().toString(); return value.replaceAll("[^A-Za-z0-9._ -]", "_"); }
    private String extension(String name) { int dot = name.lastIndexOf('.'); return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT); }
    private boolean matchesSignature(String extension, byte[] bytes) { return extension.equals("pdf") ? starts(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII)) : extension.equals("jpg") ? bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255 : starts(bytes, new byte[]{(byte) 137, 80, 78, 71}); }
    private boolean starts(byte[] value, byte[] prefix) { if (value.length < prefix.length) return false; for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false; return true; }
    private int countPdfPages(byte[] bytes) {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            if (pdf.isEncrypted()) throw new IllegalArgumentException("Password-protected PDFs are not supported");
            int pages = pdf.getNumberOfPages();
            if (pages < 1) throw new IllegalArgumentException("The PDF contains no printable pages");
            return pages;
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException error) {
            throw new IllegalArgumentException("Password-protected PDFs are not supported");
        } catch (IOException error) {
            throw new IllegalArgumentException("The PDF is malformed or unreadable");
        }
    }
    private String contentType(String extension) { return extension.equals("pdf") ? "application/pdf" : extension.equals("jpg") ? "image/jpeg" : "image/png"; }
    private static final class SetOfAllowed { private static final java.util.Set<String> EXTENSIONS = java.util.Set.of("pdf", "jpg", "png"); }
}
