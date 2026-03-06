package com.ale.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EncryptionServiceTest {

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void encryptThenDecryptReturnsOriginalPlaintext() throws Exception {
        String plaintext = "Hello, envelope encryption!";
        EncryptedPayload payload = encryptionService.encrypt(plaintext);

        assertNotNull(payload.encryptedKey());
        assertNotNull(payload.encryptedData());
        assertNotEquals(plaintext, payload.encryptedData());

        assertEquals(plaintext, encryptionService.decrypt(payload));
    }

    @Test
    void eachEncryptionProducesDifferentCiphertext() throws Exception {
        String plaintext = "same plaintext";
        EncryptedPayload first  = encryptionService.encrypt(plaintext);
        EncryptedPayload second = encryptionService.encrypt(plaintext);

        // Different DEKs and IVs must yield different ciphertexts and wrapped keys
        assertNotEquals(first.encryptedData(), second.encryptedData());
        assertNotEquals(first.encryptedKey(),  second.encryptedKey());
    }
}
