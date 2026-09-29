package com.pingprint.printprovider;

import org.junit.jupiter.api.Test;
import java.security.SecureRandom;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class TokenCipherTest {
    @Test void encryptsTokensWithRandomizedAuthenticatedEncryption() {
        byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
        TokenCipher cipher = new TokenCipher(Base64.getEncoder().encodeToString(key));
        String first = cipher.encrypt("secret-token"); String second = cipher.encrypt("secret-token");
        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("secret-token");
        assertThat(cipher.decrypt(second)).isEqualTo("secret-token");
    }

    @Test void refusesToStoreTokensWithoutAnEncryptionKey() {
        assertThatThrownBy(() -> new TokenCipher("").encrypt("secret")).hasMessageContaining("TOKEN_ENCRYPTION_KEY");
    }
}
