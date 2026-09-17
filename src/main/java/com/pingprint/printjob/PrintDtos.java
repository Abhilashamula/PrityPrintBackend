package com.pingprint.printjob;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class PrintDtos {
    private PrintDtos() { }

    public record GuestSessionRequest(
        @NotBlank String kioskId,
        @NotBlank String fileName,
        @Min(1) @Max(500) int pageCount
    ) { }

    public record GuestSessionResponse(String sessionId, boolean guest, String status, String message) { }
}