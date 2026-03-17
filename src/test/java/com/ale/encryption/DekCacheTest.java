package com.ale.encryption;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

/**
 * Verifies that the Caffeine-backed {@code dek-cache} behaves correctly:
 * <ul>
 *   <li>Cache hit: the same {@link SecretKey} instance is returned for the same wrapped key.</li>
 *   <li>Cache miss: a different wrapped key produces a different instance.</li>
 *   <li>TTL expiry: after 20 s the entry is evicted and a new instance is returned.</li>
 * </ul>
 *
 * <p>Identity comparison ({@code assertSame} / {@code assertNotSame}) is the correct signal here:
 * Caffeine returns the exact cached object reference on a hit; a fresh RSA unwrap always allocates
 * a new {@link SecretKey} instance even when the key material is identical.
 */
@SpringBootTest
class DekCacheTest {

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("dek-cache").clear();
    }

    // ── 1. Cache hit ──────────────────────────────────────────────────────────

    @Test
    void sameWrappedKey_returnsSameSecretKeyInstance() throws Exception {
        EncryptedDek dek = encryptionService.generateAndWrapDek();
        String wrappedKey = dek.wrappedKey();

        SecretKey first  = encryptionService.unwrapDek(wrappedKey);
        SecretKey second = encryptionService.unwrapDek(wrappedKey);

        assertSame(first, second,
                "Expected the same SecretKey instance on the second call (cache hit)");
    }

    // ── 2. Cache miss for a distinct wrapped key ──────────────────────────────

    @Test
    void differentWrappedKeys_returnDifferentSecretKeyInstances() throws Exception {
        String wrappedKey1 = encryptionService.generateAndWrapDek().wrappedKey();
        String wrappedKey2 = encryptionService.generateAndWrapDek().wrappedKey();

        SecretKey key1 = encryptionService.unwrapDek(wrappedKey1);
        SecretKey key2 = encryptionService.unwrapDek(wrappedKey2);

        assertNotSame(key1, key2,
                "Different wrapped keys must resolve to different SecretKey instances");
    }

    // ── 3. TTL expiry ─────────────────────────────────────────────────────────

    @Test
    void afterTtlExpiry_cacheEntryIsEvictedAndNewInstanceReturned() throws Exception {
        EncryptedDek dek = encryptionService.generateAndWrapDek();
        String wrappedKey = dek.wrappedKey();

        SecretKey before = encryptionService.unwrapDek(wrappedKey);

        // Wait for the 20-second TTL to expire (plus a small margin)
        Thread.sleep(21_000);

        SecretKey after = encryptionService.unwrapDek(wrappedKey);

        assertNotSame(before, after,
                "Expected a new SecretKey instance after the 20s TTL has expired (cache eviction)");
    }
}

