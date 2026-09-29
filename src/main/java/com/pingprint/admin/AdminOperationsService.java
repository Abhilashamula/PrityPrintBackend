package com.pingprint.admin;

import com.pingprint.document.Document;
import com.pingprint.document.DocumentService;
import com.pingprint.printer.*;
import com.pingprint.printjob.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class AdminOperationsService {
    private final NamedParameterJdbcTemplate db;
    private final PrinterRepository printers;
    private final PrinterMediaConfigRepository media;
    private final PrintOrderRepository orders;
    private final PrintJobService jobs;
    private final DocumentService documents;

    public AdminOperationsService(NamedParameterJdbcTemplate db, PrinterRepository printers,
                                  PrinterMediaConfigRepository media, PrintOrderRepository orders,
                                  PrintJobService jobs, DocumentService documents) {
        this.db = db; this.printers = printers; this.media = media; this.orders = orders; this.jobs = jobs; this.documents = documents;
    }

    public Map<String, Object> dashboard() {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM print_orders WHERE order_type = 'STUDENT') total_orders,
              (SELECT COUNT(*) FROM payment_transactions WHERE status = 'CAPTURED') successful_payments,
              (SELECT COALESCE(SUM(amount_minor),0) FROM payment_transactions WHERE status = 'CAPTURED') revenue_minor,
              (SELECT COUNT(*) FROM print_jobs WHERE status IN ('QUEUED','SUBMITTING')) queued_jobs,
              (SELECT COUNT(*) FROM print_jobs WHERE status IN ('FAILED','STATUS_UNKNOWN')) failed_jobs,
              (SELECT COUNT(*) FROM printers WHERE active AND NOT archived AND status IN ('ONLINE','BUSY')) active_printers
            """;
        return db.queryForMap(sql, Map.of());
    }

    public Map<String, Object> transactions(int page, int size, String search, String paymentStatus,
                                             String printStatus, UUID printerId, LocalDate from, LocalDate to, String direction) {
        int safePage = Math.max(0, page); int safeSize = Math.min(100, Math.max(1, size));
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (search != null && !search.isBlank()) {
            where.append(" AND (CAST(t.id AS text) ILIKE :search OR CAST(o.id AS text) ILIKE :search OR COALESCE(u.name,'') ILIKE :search OR COALESCE(u.email,'') ILIKE :search OR COALESCE(t.provider_payment_id,'') ILIKE :search) ");
            params.addValue("search", "%" + search.trim() + "%");
        }
        if (paymentStatus != null && !paymentStatus.isBlank()) { where.append(" AND t.status = :paymentStatus "); params.addValue("paymentStatus", paymentStatus.toUpperCase()); }
        if (printStatus != null && !printStatus.isBlank()) { where.append(" AND COALESCE(j.status,'NOT_CREATED') = :printStatus "); params.addValue("printStatus", printStatus.toUpperCase()); }
        if (printerId != null) { where.append(" AND p.id = :printerId "); params.addValue("printerId", printerId); }
        if (from != null) { where.append(" AND t.created_at >= :fromDate "); params.addValue("fromDate", from.atStartOfDay()); }
        if (to != null) { where.append(" AND t.created_at < :toDate "); params.addValue("toDate", to.plusDays(1).atStartOfDay()); }
        String joins = " FROM payment_transactions t JOIN print_orders o ON o.id=t.order_id JOIN printers p ON p.id=o.printer_id LEFT JOIN app_users u ON u.id=o.user_id LEFT JOIN print_jobs j ON j.order_id=o.id ";
        long total = db.queryForObject("SELECT COUNT(*)" + joins + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", safePage * safeSize);
        String order = "asc".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        String select = """
            SELECT t.id transaction_id, o.id order_id, t.provider_order_id razorpay_order_id,
              t.provider_payment_id razorpay_payment_id, COALESCE(u.name,'Guest/System') student_name,
              u.email student_email, p.id printer_id, p.name printer_name, p.location printer_location,
              t.amount_minor, o.currency, t.status payment_status, o.status order_status,
              COALESCE(j.status,'NOT_CREATED') print_status, t.created_at, o.paid_at,
              COALESCE(t.failure_reason,j.failure_reason) failure_reason, j.id print_job_id
            """;
        List<Map<String, Object>> items = db.queryForList(select + joins + where + " ORDER BY t.created_at " + order + " LIMIT :limit OFFSET :offset", params);
        return Map.of("items", items, "page", safePage, "size", safeSize, "total", total, "totalPages", (total + safeSize - 1) / safeSize);
    }

    public Map<String, Object> transaction(UUID id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        String sql = """
            SELECT t.id transaction_id, o.id order_id, t.provider_order_id razorpay_order_id,
              t.provider_payment_id razorpay_payment_id, COALESCE(u.name,'Guest/System') student_name,
              u.email student_email, p.id printer_id, p.name printer_name, p.location printer_location,
              t.amount_minor, o.currency, t.status payment_status, o.status order_status,
              COALESCE(j.status,'NOT_CREATED') print_status, t.created_at, o.paid_at,
              COALESCE(t.failure_reason,j.failure_reason) failure_reason, j.id print_job_id,
              o.file_name, o.total_pages, o.copies, o.paper_size, o.paper_type, o.color_mode
            FROM payment_transactions t JOIN print_orders o ON o.id=t.order_id
            JOIN printers p ON p.id=o.printer_id LEFT JOIN app_users u ON u.id=o.user_id
            LEFT JOIN print_jobs j ON j.order_id=o.id WHERE t.id=:id
            """;
        List<Map<String, Object>> rows = db.queryForList(sql, params);
        if (rows.isEmpty()) throw new IllegalArgumentException("Transaction not found");
        return rows.get(0);
    }

    public Map<String, Object> printerJobs(UUID printerId, int page, int size) {
        int safePage = Math.max(0, page); int safeSize = Math.min(100, Math.max(1, size));
        MapSqlParameterSource params = new MapSqlParameterSource("printerId", printerId).addValue("limit", safeSize).addValue("offset", safePage * safeSize);
        long total = db.queryForObject("SELECT COUNT(*) FROM print_jobs j JOIN print_orders o ON o.id=j.order_id WHERE o.printer_id=:printerId", params, Long.class);
        List<Map<String, Object>> items = db.queryForList("""
            SELECT j.id, j.order_id, j.provider, j.provider_job_id, j.status, j.attempt_count,
              j.failure_reason, j.created_at, j.started_at, j.completed_at, o.order_type
            FROM print_jobs j JOIN print_orders o ON o.id=j.order_id
            WHERE o.printer_id=:printerId ORDER BY j.created_at DESC LIMIT :limit OFFSET :offset
            """, params);
        return Map.of("items", items, "page", safePage, "size", safeSize, "total", total, "totalPages", (total + safeSize - 1) / safeSize);
    }

    @Transactional
    public PrintJob submitTest(UUID printerId) {
        Printer printer = printers.findById(printerId).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        if (printer.isArchived() || !printer.isActive()) throw new IllegalStateException("Enable the printer before sending a test print");
        PrinterMediaConfig selected = media.findByPrinterIdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(printerId).stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Configure and enable loaded media before sending a test print"));
        Document document = documents.storeSystemTestPdf(printer.getName());
        PrintOrder order = orders.save(PrintOrder.test(printer, document, selected));
        PrintJob job = jobs.enqueueOnce(order);
        audit("TEST_PRINT_SUBMITTED", "PRINTER", printerId, "Queued test print " + job.getId());
        return job;
    }

    public void audit(String action, String targetType, UUID targetId, String summary) {
        db.update("INSERT INTO admin_audit_log(action,target_type,target_id,summary) VALUES (:action,:type,:id,:summary)",
            new MapSqlParameterSource().addValue("action", action).addValue("type", targetType).addValue("id", targetId).addValue("summary", summary));
    }
}
