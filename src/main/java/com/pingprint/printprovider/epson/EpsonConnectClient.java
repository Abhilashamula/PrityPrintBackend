package com.pingprint.printprovider.epson;

import com.fasterxml.jackson.databind.JsonNode;
import com.pingprint.printprovider.ProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class EpsonConnectClient {
    private final String apiKey;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authBase;
    private final String apiBase;
    private final RestClient client;

    public record TokenResponse(String access_token, String refresh_token, long expires_in) { }
    public record CreatedJob(String jobId, String uploadUri) { }

    public EpsonConnectClient(
        @Value("${app.epson.api-key:}") String apiKey,
        @Value("${app.epson.client-id:}") String clientId,
        @Value("${app.epson.client-secret:}") String clientSecret,
        @Value("${app.epson.redirect-uri:}") String redirectUri,
        @Value("${app.epson.auth-base:https://auth.epsonconnect.com/auth}") String authBase,
        @Value("${app.epson.api-base:https://api.epsonconnect.com/api/2}") String apiBase,
        @Value("${app.epson.connect-timeout-ms:5000}") int connectTimeout,
        @Value("${app.epson.read-timeout-ms:20000}") int readTimeout
    ) {
        this.apiKey = apiKey; this.clientId = clientId; this.clientSecret = clientSecret; this.redirectUri = redirectUri;
        this.authBase = authBase; this.apiBase = apiBase;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout); factory.setReadTimeout(readTimeout);
        this.client = RestClient.builder().requestFactory(factory).build();
    }
    public boolean isConfigured() { return !apiKey.isBlank() && !clientId.isBlank() && !clientSecret.isBlank() && !redirectUri.isBlank(); }
    public String authorizationUrl(String state) {
        requireConfigured();
        return UriComponentsBuilder.fromUriString(authBase + "/authorize")
            .queryParam("response_type", "code").queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri).queryParam("scope", "device").queryParam("state", state)
            .build().encode().toUriString();
    }
    public TokenResponse exchangeCode(String code) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code"); form.add("code", code); form.add("redirect_uri", redirectUri); form.add("client_id", clientId);
        return token(form);
    }
    public TokenResponse refresh(String refreshToken) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token"); form.add("refresh_token", refreshToken);
        return token(form);
    }
    public JsonNode deviceInfo(String token) { return get("/printing/devices/info", token); }
    public JsonNode capabilities(String token, String mode) { return get("/printing/capability/" + mode, token); }
    public CreatedJob createJob(String token, Map<String, Object> body) {
        JsonNode response = post("/printing/jobs", token, body, true);
        String jobId = response.path("jobId").asText(); String uploadUri = response.path("uploadUri").asText();
        if (jobId.isBlank() || uploadUri.isBlank()) throw new ProviderException("Epson returned an incomplete job response", true);
        return new CreatedJob(jobId, uploadUri);
    }
    public void upload(String uploadUri, String extension, String contentType, byte[] bytes) {
        String separator = uploadUri.contains("?") ? "&" : "?";
        String uri = uploadUri + separator + "File=" + URLEncoder.encode("1." + extension, StandardCharsets.UTF_8);
        try {
            client.post().uri(uri).contentType(MediaType.parseMediaType(contentType)).body(bytes).retrieve().toBodilessEntity();
        } catch (HttpStatusCodeException error) { throw new ProviderException("Epson rejected the document upload", false, error); }
        catch (ResourceAccessException error) { throw new ProviderException("Epson document upload timed out; verify the provider job before retrying", true, error); }
    }
    public void execute(String token, String jobId) { postEmpty("/printing/jobs/" + jobId + "/print", token, true); }
    public JsonNode jobInfo(String token, String jobId) { return get("/printing/jobs/" + jobId, token); }

    private TokenResponse token(LinkedMultiValueMap<String, String> form) {
        requireConfigured();
        try {
            return client.post().uri(authBase + "/token").headers(h -> h.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(TokenResponse.class);
        } catch (HttpStatusCodeException error) { throw new ProviderException("Epson authorization was rejected", false, error); }
        catch (ResourceAccessException error) { throw new ProviderException("Epson authorization service is unavailable", false, error); }
    }
    private JsonNode get(String path, String token) {
        requireConfigured();
        try { return client.get().uri(apiBase + path).headers(h -> { h.setBearerAuth(token); h.set("x-api-key", apiKey); }).retrieve().body(JsonNode.class); }
        catch (HttpStatusCodeException error) { throw new ProviderException("Epson request was rejected: " + error.getStatusCode(), false, error); }
        catch (ResourceAccessException error) { throw new ProviderException("Epson request timed out", true, error); }
    }
    private JsonNode post(String path, String token, Object body, boolean ambiguousOnTimeout) {
        requireConfigured();
        try { return client.post().uri(apiBase + path).headers(h -> { h.setBearerAuth(token); h.set("x-api-key", apiKey); })
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class); }
        catch (HttpStatusCodeException error) { throw new ProviderException("Epson request was rejected: " + error.getStatusCode(), false, error); }
        catch (ResourceAccessException error) { throw new ProviderException("Epson request timed out", ambiguousOnTimeout, error); }
    }
    private void postEmpty(String path, String token, boolean ambiguousOnTimeout) {
        requireConfigured();
        try { client.post().uri(apiBase + path).headers(h -> { h.setBearerAuth(token); h.set("x-api-key", apiKey); })
            .retrieve().toBodilessEntity(); }
        catch (HttpStatusCodeException error) { throw new ProviderException("Epson request was rejected: " + error.getStatusCode(), false, error); }
        catch (ResourceAccessException error) { throw new ProviderException("Epson request timed out", ambiguousOnTimeout, error); }
    }
    private void requireConfigured() { if (!isConfigured()) throw new IllegalStateException("Epson Connect credentials are not configured"); }
}
