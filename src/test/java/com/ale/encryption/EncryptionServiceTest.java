package com.ale.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.UUID;
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

    // ── Tenant-scoped hash tests ───────────────────────────────────────────────

    @Test
    void tenantHashIsDeterministicForSameTenantAndValue() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String h1 = encryptionService.hash("Smith", tenantId);
        String h2 = encryptionService.hash("Smith", tenantId);
        assertEquals(h1, h2, "Same tenant + same value must produce the same hash");
    }

    @Test
    void tenantHashDiffersAcrossTenants() throws Exception {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        String h1 = encryptionService.hash("Smith", tenant1);
        String h2 = encryptionService.hash("Smith", tenant2);
        assertNotEquals(h1, h2, "Same value stored under different tenants must produce different hashes");
    }

    @Test
    void tenantHashDiffersFromGlobalHash() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String global = encryptionService.hash("Smith");
        String scoped = encryptionService.hash("Smith", tenantId);
        assertNotEquals(global, scoped,
                "Tenant-scoped hash must differ from the global (non-tenant) hash");
    }

    @Test
    void tenantHashReturnsNullForNullValue() throws Exception {
        assertNotNull(UUID.randomUUID()); // sanity
        assertEquals(null, encryptionService.hash(null, UUID.randomUUID()));
    }
}
