package com.pingprint.printer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PrinterCapabilityService {
    private final PrinterRepository printers;
    private final PrinterMediaConfigRepository media;
    private final PrinterProviderConnectionRepository connections;
    private final ObjectMapper json;
    private final JdbcTemplate db;

    public record MediaOption(UUID id, String paperSize, String paperType, List<String> colorModes, boolean duplexSupported, long priceBwMinor, long priceColorMinor) { }
    public record Capabilities(UUID printerId, boolean available, List<MediaOption> mediaOptions) { }
    public record ConfigureMedia(UUID id, String paperSource, String paperSize, String paperType, String printQuality, boolean borderless,
                                 boolean duplexSupported, boolean colorSupported, boolean monoSupported, boolean enabled, long priceBwMinor, long priceColorMinor) { }

    public PrinterCapabilityService(PrinterRepository printers, PrinterMediaConfigRepository media, PrinterProviderConnectionRepository connections, ObjectMapper json, JdbcTemplate db) {
        this.printers = printers; this.media = media; this.connections = connections; this.json = json; this.db = db;
    }
    public Capabilities studentCapabilities(UUID printerId) {
        Printer printer = printers.findById(printerId).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        boolean available = printer.isActive() && (printer.getStatus() == PrinterStatus.ONLINE || printer.getStatus() == PrinterStatus.BUSY);
        List<MediaOption> options = media.findByPrinterIdAndEnabledTrueOrderByPaperSizeAscPaperTypeAsc(printerId).stream().map(item -> {
            List<String> colors = new ArrayList<>(); if (item.isMonoSupported()) colors.add("MONO"); if (item.isColorSupported()) colors.add("COLOR");
            return new MediaOption(item.getId(), displayValue(item.getPaperSize(), "ps_"), displayValue(item.getPaperType(), "pt_"), colors,
                item.isDuplexSupported(), item.getPriceBwMinor(), item.getPriceColorMinor());
        }).toList();
        return new Capabilities(printerId, available && !options.isEmpty(), options);
    }
    public List<ConfigureMedia> adminMedia(UUID printerId) {
        return media.findByPrinterIdOrderByPaperSizeAscPaperTypeAsc(printerId).stream().map(this::dto).toList();
    }
    @Transactional
    public ConfigureMedia configure(UUID printerId, ConfigureMedia request) {
        Printer printer = printers.findById(printerId).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        requirePositive(request.priceBwMinor(), "B&W price"); requirePositive(request.priceColorMinor(), "Color price");
        if ("EPSON_CONNECT".equals(printer.getProvider())) validateEpsonCapability(printerId, request);
        PrinterMediaConfig item = request.id() == null
            ? new PrinterMediaConfig(printer, required(request.paperSource(), "Paper source"), required(request.paperSize(), "Paper size"), required(request.paperType(), "Paper type"), required(request.printQuality(), "Print quality"))
            : media.findById(request.id()).filter(value -> value.getPrinterId().equals(printerId)).orElseThrow(() -> new IllegalArgumentException("Media configuration not found"));
        item.configure(request.enabled(), request.duplexSupported(), request.colorSupported(), request.monoSupported(), request.borderless(), request.priceBwMinor(), request.priceColorMinor(), request.printQuality());
        return dto(media.save(item));
    }
    private void validateEpsonCapability(UUID printerId, ConfigureMedia request) {
        String raw = connections.findByPrinterId(printerId).map(PrinterProviderConnection::getCapabilitiesJson).orElseThrow(() -> new IllegalStateException("Connect Epson and refresh capabilities first"));
        try {
            JsonNode root = json.readTree(raw);
            boolean colorAllowed = contains(root.path("colorModes"), "color"); boolean monoAllowed = contains(root.path("colorModes"), "mono");
            for (JsonNode size : root.path("paperSizes")) if (size.path("paperSize").asText().equals(request.paperSize())) {
                for (JsonNode type : size.path("paperTypes")) if (type.path("paperType").asText().equals(request.paperType())) {
                    boolean valid = contains(type.path("paperSources"), request.paperSource()) && contains(type.path("printQualities"), request.printQuality());
                    if (valid && request.duplexSupported() && !type.path("doubleSided").asBoolean(false)) valid = false;
                    if (valid && request.colorSupported() && !colorAllowed) valid = false;
                    if (valid && request.monoSupported() && !monoAllowed) valid = false;
                    if (valid) return;
                }
            }
            throw new IllegalArgumentException("Media setup is not present in the Epson capability response");
        } catch (IllegalArgumentException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException("Stored Epson capabilities are invalid", error); }
    }
    private boolean contains(JsonNode values, String expected) { for (JsonNode value : values) if (value.asText().equals(expected)) return true; return false; }
    private ConfigureMedia dto(PrinterMediaConfig item) { return new ConfigureMedia(item.getId(), item.getPaperSource(), item.getPaperSize(), item.getPaperType(), item.getPrintQuality(), item.isBorderless(), item.isDuplexSupported(), item.isColorSupported(), item.isMonoSupported(), item.isEnabled(), item.getPriceBwMinor(), item.getPriceColorMinor()); }
    private String displayValue(String value, String prefix) { String normalized = value.startsWith(prefix) ? value.substring(prefix.length()) : value; return normalized.replace("paper", "").replace('_', ' ').toUpperCase(Locale.ROOT); }
    private String required(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); return value; }
    private void requirePositive(long value, String name) { if (value < 1) throw new IllegalArgumentException(name + " must be positive"); }
}
