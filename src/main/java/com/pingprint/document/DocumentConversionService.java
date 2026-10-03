package com.pingprint.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentConversionService {
    private static final Set<String> OFFICE_EXTENSIONS = Set.of(
        "doc", "docx", "ppt", "pptx", "xls", "xlsx", "odt", "odp", "ods", "rtf"
    );
    private final String libreOfficeCommand;
    private final long timeoutSeconds;

    public DocumentConversionService(
        @Value("${app.conversion.libreoffice-command:libreoffice}") String libreOfficeCommand,
        @Value("${app.conversion.timeout-seconds:90}") long timeoutSeconds
    ) {
        this.libreOfficeCommand = libreOfficeCommand;
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean requiresPdfConversion(String extension) {
        return OFFICE_EXTENSIONS.contains(extension) || extension.equals("tif") || extension.equals("tiff");
    }

    public byte[] toPdf(String extension, byte[] source) {
        if (extension.equals("tif") || extension.equals("tiff")) return tiffToPdf(source);
        if (OFFICE_EXTENSIONS.contains(extension)) return officeToPdf(extension, source);
        throw new IllegalArgumentException("Unsupported conversion format");
    }

    private byte[] officeToPdf(String extension, byte[] source) {
        Path work = null;
        try {
            work = Files.createTempDirectory("pingprint-convert-");
            Path input = work.resolve("document." + extension);
            Path profile = work.resolve("profile");
            Files.write(input, source);
            Process process = new ProcessBuilder(
                libreOfficeCommand,
                "-env:UserInstallation=" + profile.toUri(),
                "--headless", "--nologo", "--nodefault", "--nolockcheck", "--nofirststartwizard",
                "--convert-to", "pdf", "--outdir", work.toString(), input.toString()
            ).redirectErrorStream(true).start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalArgumentException("Document conversion timed out");
            }
            byte[] output = process.getInputStream().readAllBytes();
            Path pdf = work.resolve("document.pdf");
            if (process.exitValue() != 0 || !Files.isRegularFile(pdf) || Files.size(pdf) == 0) {
                String detail = new String(output, java.nio.charset.StandardCharsets.UTF_8).trim();
                throw new IllegalArgumentException("The Office document could not be converted" + (detail.isBlank() ? "" : ": " + detail));
            }
            return Files.readAllBytes(pdf);
        } catch (IOException error) {
            throw new IllegalStateException("Document conversion service is unavailable", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Document conversion was interrupted", error);
        } finally {
            deleteTree(work);
        }
    }

    private byte[] tiffToPdf(byte[] source) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source));
             PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("The TIFF file is malformed or unsupported");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, false);
                int count = reader.getNumImages(true);
                if (count < 1) throw new IllegalArgumentException("The TIFF file contains no printable pages");
                for (int index = 0; index < count; index++) {
                    BufferedImage image = reader.read(index);
                    float scale = Math.min(1f, 14000f / Math.max(image.getWidth(), image.getHeight()));
                    float width = Math.max(1, image.getWidth() * scale);
                    float height = Math.max(1, image.getHeight() * scale);
                    PDPage page = new PDPage(new PDRectangle(width, height));
                    pdf.addPage(page);
                    PDImageXObject embedded = LosslessFactory.createFromImage(pdf, image);
                    try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                        content.drawImage(embedded, 0, 0, width, height);
                    }
                }
                pdf.save(output);
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("The TIFF file is malformed or unreadable", error);
        }
    }

    private void deleteTree(Path root) {
        if (root == null) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
