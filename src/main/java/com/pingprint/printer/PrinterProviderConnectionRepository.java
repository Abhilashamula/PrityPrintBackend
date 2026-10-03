package com.pingprint.printer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PrinterProviderConnectionRepository extends JpaRepository<PrinterProviderConnection, UUID> {
    Optional<PrinterProviderConnection> findByPrinter_Id(UUID printerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PrinterProviderConnection c where c.printer.id = :printerId")
    Optional<PrinterProviderConnection> findByPrinterIdForUpdate(UUID printerId);
}
