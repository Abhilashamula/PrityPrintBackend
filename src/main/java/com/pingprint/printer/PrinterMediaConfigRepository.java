package com.pingprint.printer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrinterMediaConfigRepository extends JpaRepository<PrinterMediaConfig, UUID> {
    List<PrinterMediaConfig> findByPrinterIdOrderByPaperSizeAscPaperTypeAsc(UUID printerId);
    List<PrinterMediaConfig> findByPrinterIdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(UUID printerId);
    Optional<PrinterMediaConfig> findByIdAndPrinterIdAndEnabledTrue(UUID id, UUID printerId);
}
