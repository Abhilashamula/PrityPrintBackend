package com.pingprint.document;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file, Authentication authentication) {
        Document document = service.store(userId(authentication), file);
        return Map.of("id", document.getId(), "fileName", document.getOriginalFileName(), "pageCount", document.getPageCount(), "contentType", document.getContentType());
    }
    private UUID userId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof UUID id ? id : null;
    }
}