package com.pingprint.printprovider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenCipher {
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public TokenCipher(@Value("${app.token-encryption-key:}") String encodedKey) {
        key = encodedKey.isBlank() ? null : Base64.getDecoder().decode(encodedKey);
        if (key != null && key.length != 32) throw new IllegalArgumentException("TOKEN_ENCRYPTION_KEY must be a base64-encoded 32-byte key");
    }
    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length); System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception error) { throw new IllegalStateException("Could not encrypt provider token", error); }
    }
    public String decrypt(String ciphertext) {
        requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length < 29) throw new IllegalArgumentException("Invalid encrypted token");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, combined, 0, 12));
            return new String(cipher.doFinal(combined, 12, combined.length - 12), StandardCharsets.UTF_8);
        } catch (Exception error) { throw new IllegalStateException("Could not decrypt provider token", error); }
    }
    public boolean isConfigured() { return key != null; }
    private void requireKey() { if (key == null) throw new IllegalStateException("TOKEN_ENCRYPTION_KEY is required for printer authorization"); }
}
