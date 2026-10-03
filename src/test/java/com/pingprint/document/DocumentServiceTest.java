package com.pingprint.document;

import com.pingprint.user.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentServiceTest {
    @TempDir Path storage;

    @Test
    void countsEveryPageInPdf() throws Exception {
        DocumentService service = service();
        byte[] pdfBytes;
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int page = 0; page < 6; page++) pdf.addPage(new PDPage());
            pdf.save(output);
            pdfBytes = output.toByteArray();
        }

        Document stored = service.store(null,
            new MockMultipartFile("file", "six-pages.pdf", "application/pdf", pdfBytes));

        assertEquals(6, stored.getPageCount());
    }

    @Test
    void treatsEachSupportedImageAsOnePrintablePage() {
        DocumentService service = service();
        Document jpg = service.store(null, new MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0}));
        Document png = service.store(null, new MockMultipartFile(
            "file", "image.png", "image/png", new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}));

        assertEquals(1, jpg.getPageCount());
        assertEquals(1, png.getPageCount());
    }

    @Test
    void countsPagesFromNormalizedOfficePdf() throws Exception {
        DocumentRepository documents = mock(DocumentRepository.class);
        when(documents.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentConversionService conversion = mock(DocumentConversionService.class);
        byte[] source = new byte[]{80, 75, 3, 4, 0};
        when(conversion.requiresPdfConversion("docx")).thenReturn(true);
        when(conversion.toPdf("docx", source)).thenReturn(pdfWithPages(4));
        DocumentService service = new DocumentService(documents, mock(UserRepository.class), conversion, storage.toString());

        Document stored = service.store(null,
            new MockMultipartFile("file", "assignment.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", source));

        assertEquals(4, stored.getPageCount());
        assertEquals("application/pdf", stored.getContentType());
        org.junit.jupiter.api.Assertions.assertTrue(stored.getStorageKey().endsWith(".pdf"));
    }

    private DocumentService service() {
        DocumentRepository documents = mock(DocumentRepository.class);
        when(documents.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new DocumentService(documents, mock(UserRepository.class), mock(DocumentConversionService.class), storage.toString());
    }

    private byte[] pdfWithPages(int pages) throws Exception {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int page = 0; page < pages; page++) pdf.addPage(new PDPage());
            pdf.save(output);
            return output.toByteArray();
        }
    }
}
