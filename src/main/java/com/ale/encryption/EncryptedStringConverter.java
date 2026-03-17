package com.ale.encryption;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA {@link AttributeConverter} that applies envelope encryption to every {@code String} field
 * it is attached to.
 *
 * <h3>Storage format</h3>
 * Each column value is stored as a single {@code TEXT} string:
 * <pre>
 *   &lt;base64-wrappedDEK&gt;|&lt;base64-IV+ciphertext&gt;
 * </pre>
 * The separator {@code |} is safe because Base64 strings never contain it.
 *
 * <h3>Per-tenant DEK reuse</h3>
 * Each tenant is assigned its own rotating AES-256 DEK.  A fresh DEK is generated on first use
 * for a given tenant and reused for up to {@value #DEK_REUSE_LIMIT} encryption calls before
 * being automatically rotated.  The wrapped DEK is stored in every record so decryption is
 * always self-contained — records encrypted under different DEKs are handled transparently.
 *
 * <p>The active {@code tenantId} is read from {@link TenantContext} (a thread-local set by
 * controllers and services before flushing JPA writes).  An {@link IllegalStateException} is
 * thrown if the context is not populated.
 *
 * <h3>Thread-safety</h3>
 * A per-tenant {@link ReentrantLock} guards each tenant's DEK state so concurrent inserts
 * from different tenants do not interfere with each other.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    /** Number of encryption calls that may share a single DEK before it is rotated. */
    static final int DEK_REUSE_LIMIT = 1000;

    private static final char SEPARATOR = '|';

    @Autowired
    private EncryptionService encryptionService;

    // ── Per-tenant DEK state ───────────────────────────────────────────────────
    private final ConcurrentHashMap<UUID, ReentrantLock> tenantLocks  = new ConcurrentHashMap<>();
    // Accessed only while holding the corresponding per-tenant lock:
    private final HashMap<UUID, EncryptedDek> tenantDeks        = new HashMap<>();
    private final HashMap<UUID, Integer>      tenantUsageCounts = new HashMap<>();

    // ── Encryption ─────────────────────────────────────────────────────────────

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new IllegalStateException(
                "TenantContext is not set. Set TenantContext.set(tenantId) before persisting ALEUser entities.");
        }
        try {
            ReentrantLock lock = tenantLocks.computeIfAbsent(tenantId, k -> new ReentrantLock());
            lock.lock();
            try {
                rotateDekIfNeeded(tenantId);
                EncryptedDek dek = tenantDeks.get(tenantId);
                String encryptedData = encryptionService.encryptWithDek(attribute, dek.rawDek());
                tenantUsageCounts.merge(tenantId, 1, Integer::sum);
                return dek.wrappedKey() + SEPARATOR + encryptedData;
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt field value", e);
        }
    }

    // ── Decryption ─────────────────────────────────────────────────────────────

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            int sep = dbData.indexOf(SEPARATOR);
            if (sep < 0) {
                throw new IllegalArgumentException("Encrypted field is missing separator '|'");
            }
            String wrappedKey    = dbData.substring(0, sep);
            String encryptedData = dbData.substring(sep + 1);
            return encryptionService.decryptField(wrappedKey, encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt field value", e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Must be called while holding the per-tenant lock. */
    private void rotateDekIfNeeded(UUID tenantId) throws Exception {
        EncryptedDek current = tenantDeks.get(tenantId);
        int usage = tenantUsageCounts.getOrDefault(tenantId, 0);
        if (current == null || usage >= DEK_REUSE_LIMIT) {
            tenantDeks.put(tenantId, encryptionService.generateAndWrapDek());
            tenantUsageCounts.put(tenantId, 0);
        }
    }
}
