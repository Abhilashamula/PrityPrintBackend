package com.pingprint.printjob;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/print")
public class PrintJobController {
    @PostMapping("/guest-session")
    @ResponseStatus(HttpStatus.CREATED)
    public PrintDtos.GuestSessionResponse createGuestSession(@Valid @RequestBody PrintDtos.GuestSessionRequest request) {
        return new PrintDtos.GuestSessionResponse(
            "guest_" + UUID.randomUUID(),
            true,
            "created",
            "Guest print session created. You can upload and configure the document without signing in."
        );
    }

    @GetMapping("/guest-session/{sessionId}")
    public Map<String, Object> guestSession(@PathVariable String sessionId) {
        return Map.of("sessionId", sessionId, "guest", true, "status", "created");
    }
}