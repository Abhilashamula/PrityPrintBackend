package com.pingprint.printer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrinterMediaConfigRepository extends JpaRepository<PrinterMediaConfig, UUID> {
    List<PrinterMediaConfig> findByPrinter_IdOrderByPaperSizeAscPaperTypeAsc(UUID printerId);
    List<PrinterMediaConfig> findByPrinter_IdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(UUID printerId);
    Optional<PrinterMediaConfig> findByIdAndPrinter_IdAndEnabledTrue(UUID id, UUID printerId);
    List<PrinterMediaConfig> findByPrinter_IdAndPaperSourceAndEnabledTrue(UUID printerId, String paperSource);
}
