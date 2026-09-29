package com.pingprint.printer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PrinterProviderConnectionRepository extends JpaRepository<PrinterProviderConnection, UUID> {
    Optional<PrinterProviderConnection> findByPrinterId(UUID printerId);
}
