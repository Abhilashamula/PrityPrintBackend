package com.pingprint.printprovider.epson;

import com.fasterxml.jackson.databind.JsonNode;
import com.pingprint.printer.*;
import com.pingprint.printprovider.ProviderException;
import com.pingprint.printprovider.TokenCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EpsonAuthService {
    private final EpsonConnectClient client;
    private final TokenCipher cipher;
    private final PrinterRepository printers;
    private final PrinterProviderConnectionRepository connections;
    private final EpsonOAuthStateRepository states;

    public EpsonAuthService(EpsonConnectClient client, TokenCipher cipher, PrinterRepository printers, PrinterProviderConnectionRepository connections, EpsonOAuthStateRepository states) {
        this.client = client; this.cipher = cipher; this.printers = printers; this.connections = connections; this.states = states;
    }
    @Transactional
    public String begin(UUID printerId) {
        Printer printer = printers.findById(printerId).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        if (!"EPSON_CONNECT".equals(printer.getProvider())) throw new IllegalArgumentException("Printer provider is not Epson Connect");
        if (!cipher.isConfigured()) throw new IllegalStateException("TOKEN_ENCRYPTION_KEY is required before connecting Epson");
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes());
        states.save(new EpsonOAuthState(hash(state), printerId));
        return client.authorizationUrl(state);
    }
    @Transactional
    public UUID complete(String code, String state) {
        EpsonOAuthState stored = states.findById(hash(state)).orElseThrow(() -> new IllegalArgumentException("Invalid Epson OAuth state"));
        states.delete(stored);
        if (stored.isExpired()) throw new IllegalArgumentException("Epson OAuth state expired");
        Printer printer = printers.findById(stored.getPrinterId()).orElseThrow(() -> new IllegalArgumentException("Printer not found"));
        EpsonConnectClient.TokenResponse tokens = client.exchangeCode(code);
        PrinterProviderConnection connection = connections.findByPrinterId(printer.getId()).orElseGet(() -> new PrinterProviderConnection(printer, "EPSON_CONNECT"));
        Instant now = Instant.now();
        connection.authorize(cipher.encrypt(tokens.access_token()), cipher.encrypt(tokens.refresh_token()), now.plusSeconds(tokens.expires_in()), now.plus(30, ChronoUnit.DAYS));
        JsonNode device = client.deviceInfo(tokens.access_token());
        JsonNode capabilities = client.capabilities(tokens.access_token(), "document");
        connection.updateDevice(device.path("serialNumber").asText(null), device.path("connected").asBoolean(false));
        connection.updateCapabilities(capabilities.toString());
        printer.updateDeviceInfo("Epson", device.path("productName").asText("Epson printer"), device.path("connected").asBoolean(false));
        connections.save(connection); printers.save(printer);
        return printer.getId();
    }
    @Transactional
    public String validAccessToken(UUID printerId) {
        PrinterProviderConnection connection = connections.findByPrinterId(printerId).orElseThrow(() -> new IllegalStateException("Epson printer is not authorized"));
        if (connection.isReauthorizationRequired()) throw new IllegalStateException("Epson printer requires reauthorization");
        if (connection.getAccessTokenExpiresAt().isAfter(Instant.now().plusSeconds(90))) return cipher.decrypt(connection.getEncryptedAccessToken());
        if (connection.getRefreshTokenExpiresAt() != null && connection.getRefreshTokenExpiresAt().isBefore(Instant.now())) {
            connection.requireReauthorization(); connections.save(connection); throw new IllegalStateException("Epson authorization expired; reconnect the printer");
        }
        try {
            EpsonConnectClient.TokenResponse tokens = client.refresh(cipher.decrypt(connection.getEncryptedRefreshToken()));
            String refresh = tokens.refresh_token() == null || tokens.refresh_token().isBlank() ? cipher.decrypt(connection.getEncryptedRefreshToken()) : tokens.refresh_token();
            Instant now = Instant.now();
            connection.updateTokens(cipher.encrypt(tokens.access_token()), cipher.encrypt(refresh), now.plusSeconds(tokens.expires_in()), now.plus(30, ChronoUnit.DAYS));
            connections.save(connection);
            return tokens.access_token();
        } catch (ProviderException error) {
            connection.requireReauthorization(); connections.save(connection); throw error;
        }
    }
    @Transactional
    public void refreshCapabilities(UUID printerId) {
        PrinterProviderConnection connection = connections.findByPrinterId(printerId).orElseThrow(() -> new IllegalArgumentException("Epson connection not found"));
        connection.updateCapabilities(client.capabilities(validAccessToken(printerId), "document").toString());
        connections.save(connection);
    }
    private byte[] randomBytes() { byte[] value = new byte[32]; new java.security.SecureRandom().nextBytes(value); return value; }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
}
