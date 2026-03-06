package com.ale.encryption;

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
 * <h3>DEK reuse</h3>
 * A fresh AES-256 DEK is generated once and reused for up to {@value #DEK_REUSE_LIMIT}
 * encryption calls before being rotated.  The wrapped DEK is stored alongside every record so
 * decryption is always self-contained; records encrypted under different DEKs are handled
 * transparently.
 *
 * <h3>Thread-safety</h3>
 * A {@link ReentrantLock} guards the DEK state so concurrent inserts stay consistent.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    /** Number of encryption calls that may share a single DEK before it is rotated. */
    static final int DEK_REUSE_LIMIT = 1000;

    private static final char SEPARATOR = '|';

    @Autowired
    private EncryptionService encryptionService;

    // ── DEK state (guarded by dekLock) ────────────────────────────────────────
    private final ReentrantLock dekLock = new ReentrantLock();
    private EncryptedDek currentDek;
    private int usageCount = 0;

    // ── Encryption ─────────────────────────────────────────────────────────────

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            dekLock.lock();
            try {
                rotateDekIfNeeded();
                String encryptedData = encryptionService.encryptWithDek(attribute, currentDek.rawDek());
                usageCount++;
                return currentDek.wrappedKey() + SEPARATOR + encryptedData;
            } finally {
                dekLock.unlock();
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

    /** Must be called while holding {@code dekLock}. */
    private void rotateDekIfNeeded() throws Exception {
        if (currentDek == null || usageCount >= DEK_REUSE_LIMIT) {
            currentDek = encryptionService.generateAndWrapDek();
            usageCount = 0;
        }
    }
}
