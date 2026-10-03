package com.pingprint.printer;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PrinterAvailabilityServiceTest {
    private final PrinterProviderConnectionRepository connections = mock(PrinterProviderConnectionRepository.class);
    private final PrinterMediaConfigRepository media = mock(PrinterMediaConfigRepository.class);
    private final PrinterAvailabilityService service = new PrinterAvailabilityService(connections, media);

    @Test void localAgentIsNeverOfferedWithoutAProviderImplementation() {
        Printer printer = printer("LOCAL_AGENT", true, PrinterStatus.ONLINE);
        assertThat(service.studentAvailabilityIssues(printer)).contains("No production print provider is configured");
    }

    @Test void disconnectedEpsonIsNotAvailableEvenIfStatusWasSetOnline() {
        Printer printer = printer("EPSON_CONNECT", true, PrinterStatus.ONLINE);
        when(connections.findByPrinter_Id(printer.getId())).thenReturn(Optional.empty());
        assertThat(service.isStudentAvailable(printer)).isFalse();
    }

    @Test void connectedOnlineEpsonWithLoadedMediaIsAvailable() {
        Printer printer = printer("EPSON_CONNECT", true, PrinterStatus.ONLINE);
        PrinterProviderConnection connection = mock(PrinterProviderConnection.class);
        when(connection.isConnected()).thenReturn(true);
        when(connections.findByPrinter_Id(printer.getId())).thenReturn(Optional.of(connection));
        when(media.findByPrinter_IdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(printer.getId()))
            .thenReturn(List.of(mock(PrinterMediaConfig.class)));
        assertThat(service.isStudentAvailable(printer)).isTrue();
    }

    private Printer printer(String provider, boolean active, PrinterStatus status) {
        Printer printer = mock(Printer.class);
        when(printer.getId()).thenReturn(UUID.randomUUID());
        when(printer.getProvider()).thenReturn(provider);
        when(printer.isActive()).thenReturn(active);
        when(printer.getStatus()).thenReturn(status);
        when(media.findByPrinter_IdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(printer.getId())).thenReturn(List.of());
        return printer;
    }
}
