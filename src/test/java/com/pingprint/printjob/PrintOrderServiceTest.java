package com.pingprint.printjob;

import com.pingprint.document.*;
import com.pingprint.printer.*;
import com.pingprint.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrintOrderServiceTest {
    private final PrintOrderRepository orders = mock(PrintOrderRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final PrinterRepository printers = mock(PrinterRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PrinterMediaConfigRepository media = mock(PrinterMediaConfigRepository.class);
    private PrintOrderService service;
    private UUID userId;
    private UUID printerId;
    private UUID documentId;
    private UUID mediaId;

    @BeforeEach void setup() {
        service = new PrintOrderService(orders, documents, printers, users, mock(JdbcTemplate.class), media);
        User user = new User("student@example.com", "hash", "Student", 20, "other", "9999999999");
        userId = UUID.randomUUID(); ReflectionTestUtils.setField(user, "id", userId);
        Printer printer = Printer.cloud("L18050", "Lab", "EPSON_CONNECT");
        printerId = UUID.randomUUID(); ReflectionTestUtils.setField(printer, "id", printerId); printer.setStatus(PrinterStatus.ONLINE);
        Document document = new Document(user, "key.pdf", "notes.pdf", "application/pdf", 100, 5);
        documentId = UUID.randomUUID(); ReflectionTestUtils.setField(document, "id", documentId);
        PrinterMediaConfig config = new PrinterMediaConfig(printer, "rear", "ps_a4", "pt_plainpaper", "normal");
        mediaId = UUID.randomUUID(); ReflectionTestUtils.setField(config, "id", mediaId); config.configure(true, false, true, true, false, 200, 500, "normal");
        when(users.findById(userId)).thenReturn(Optional.of(user)); when(printers.findById(printerId)).thenReturn(Optional.of(printer));
        when(documents.findById(documentId)).thenReturn(Optional.of(document)); when(media.findByIdAndPrinter_IdAndEnabledTrue(mediaId, printerId)).thenReturn(Optional.of(config));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void calculatesAuthoritativePriceFromStoredDocumentAndMedia() {
        PrintOrder order = service.create(userId, request("COLOR", false, null));
        assertThat(order.getAmountMinor()).isEqualTo(5_000);
        assertThat(order.getTotalPages()).isEqualTo(5);
        assertThat(order.getPaperSource()).isEqualTo("rear");
    }

    @Test void createsGuestOrderForGuestDocument() {
        Document guestDocument = new Document(null, "guest/key.pdf", "notes.pdf", "application/pdf", 100, 5);
        ReflectionTestUtils.setField(guestDocument, "id", documentId);
        when(documents.findById(documentId)).thenReturn(Optional.of(guestDocument));

        PrintOrder order = service.create(null, request("BW", false, null));

        assertThat(order.getUserId()).isNull();
        assertThat(order.getOrderType()).isEqualTo("GUEST");
        assertThat(order.getAmountMinor()).isEqualTo(2_000);
    }

    @Test void rejectsUnsupportedDuplex() {
        assertThatThrownBy(() -> service.create(userId, request("BW", true, null))).hasMessageContaining("Duplex");
    }

    @Test void rejectsUnavailableMedia() {
        when(media.findByIdAndPrinter_IdAndEnabledTrue(mediaId, printerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(userId, request("BW", false, null))).hasMessageContaining("media is unavailable");
    }

    @Test void rejectsCustomPageRangeForEpsonUntilPdfExtractionExists() {
        assertThatThrownBy(() -> service.create(userId, request("BW", false, "1-2"))).hasMessageContaining("Custom page ranges");
    }

    @Test void rejectsUnverifiedLandscapeModeForEpson() {
        PrintOrderService.CreateRequest request = new PrintOrderService.CreateRequest(
            printerId, documentId, mediaId, 1, "A4", "PLAIN", "BW", false, "LANDSCAPE", null);
        assertThatThrownBy(() -> service.create(userId, request)).hasMessageContaining("Landscape orientation");
    }

    private PrintOrderService.CreateRequest request(String color, boolean duplex, String pageRange) {
        return new PrintOrderService.CreateRequest(printerId, documentId, mediaId, 2, "A4", "PLAIN", color, duplex, "PORTRAIT", pageRange);
    }
}
