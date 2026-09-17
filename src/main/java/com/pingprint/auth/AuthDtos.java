package com.pingprint.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }
    public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 72) @Pattern(regexp = "^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).+$", message = "must include an uppercase letter and a special character") String password,
        @NotBlank @Size(max = 120) String name,
        @Min(13) @Max(120) int age,
        @NotBlank @Pattern(regexp = "(?i)^(male|female|other|prefer_not_to_say)$") String gender,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "must be exactly 10 digits") String phone
    ) { }
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) { }
    public record VerifyEmailRequest(@NotBlank String token) { }
    public record GoogleSignupRequest(@NotBlank String credential, @NotBlank @Size(max = 120) String name, @Min(13) @Max(120) int age, @NotBlank @Pattern(regexp = "(?i)^(male|female|other|prefer_not_to_say)$") String gender, @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "must be exactly 10 digits") String phone) { }
    public record AuthResponse(String accessToken, UserResponse user) { }
    public record UserResponse(String id, String email, String name, int age, String gender, String phone, boolean emailVerified, WalletResponse wallet) { }
    public record WalletResponse(long balanceMinor, String currency) { }
}
