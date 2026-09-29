package com.pingprint.printer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrinterRepository extends JpaRepository<Printer, UUID> {
    List<Printer> findByStatusInOrderByNameAsc(List<PrinterStatus> statuses);
    List<Printer> findByActiveTrueAndStatusInOrderByNameAsc(List<PrinterStatus> statuses);
}
