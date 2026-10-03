package com.pingprint.printprovider.epson;

import com.fasterxml.jackson.databind.JsonNode;
import com.pingprint.document.Document;
import com.pingprint.document.DocumentService;
import com.pingprint.printjob.PrintJob;
import com.pingprint.printjob.PrintJobRepository;
import com.pingprint.printjob.PrintOrder;
import com.pingprint.printprovider.PrintProvider;
import com.pingprint.printprovider.ProviderException;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class EpsonConnectPrintProvider implements PrintProvider {
    private final EpsonConnectClient client;
    private final EpsonAuthService auth;
    private final DocumentService documents;
    private final PrintJobRepository jobs;
    public EpsonConnectPrintProvider(EpsonConnectClient client, EpsonAuthService auth, DocumentService documents, PrintJobRepository jobs) { this.client = client; this.auth = auth; this.documents = documents; this.jobs = jobs; }
    @Override public String providerName() { return "EPSON_CONNECT"; }

    @Override public void submit(PrintJob job) {
        PrintOrder order = job.getOrder();
        if (!order.getPrinter().isActive()) throw new ProviderException("Printer was disabled before submission", false);
        String token = auth.validAccessToken(order.getPrinterId());
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("paperSize", order.getPaperSize()); settings.put("paperType", order.getPaperType());
        settings.put("borderless", false); settings.put("printQuality", order.getPrintQuality()); settings.put("paperSource", order.getPaperSource());
        settings.put("colorMode", order.getColorMode().equals("COLOR") ? "color" : "mono"); settings.put("copies", order.getCopies());
        settings.put("doubleSided", order.isDuplex());
        if (order.getPageRange() != null) throw new ProviderException("Page-range printing requires a server-side PDF extraction step and is not enabled for Epson Connect", false);
        Map<String, Object> request = Map.of("jobName", "PingPrint" + order.getId().toString().replace("-", ""), "printMode", "document", "printSettings", settings);
        EpsonConnectClient.CreatedJob created = client.createJob(token, request);
        job.recordProviderJob(created.jobId()); jobs.saveAndFlush(job);
        Document document = order.getDocument();
        String extension = document.getContentType().equals("application/pdf") ? "pdf"
            : document.getContentType().equals("image/jpeg") ? "jpg" : "png";
        client.upload(created.uploadUri(), extension, document.getContentType(), documents.read(document));
        client.execute(token, created.jobId());
        job.submitted(); jobs.save(job);
    }
    @Override public void refreshStatus(PrintJob job) {
        if (job.getProviderJobId() == null) return;
        JsonNode response = client.jobInfo(auth.validAccessToken(job.getOrder().getPrinterId()), job.getProviderJobId());
        String status = response.path("status").asText("").toLowerCase(Locale.ROOT);
        String reason = response.path("statusReason").asText(response.path("status_reason").asText(""));
        job.providerStatus(status, reason);
        if (status.equals("completed") && reason.isBlank()) job.completed();
        else if (status.equals("processing")) job.printing(status, reason);
        else if (status.equals("canceled") || (status.equals("completed") && !reason.isBlank())) job.failed(reason.isBlank() ? "Epson cancelled the print job" : reason);
        jobs.save(job);
    }
    private String extension(String name) { int dot = name.lastIndexOf('.'); return dot < 0 ? "pdf" : name.substring(dot + 1).toLowerCase(Locale.ROOT).replace("jpeg", "jpg"); }
}
