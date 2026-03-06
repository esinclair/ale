package com.ale.encryption;

/**
 * Holds the output of envelope encryption.
 *
 * <ul>
 *   <li>{@code encryptedKey}  – the ephemeral AES-256 DEK wrapped with the RSA public key
 *       (Base64, RSA/ECB/OAEPWithSHA-256AndMGF1Padding)</li>
 *   <li>{@code encryptedData} – the ciphertext produced by AES/GCM/NoPadding, prefixed with
 *       the 12-byte IV (Base64)</li>
 * </ul>
 */
public record EncryptedPayload(String encryptedKey, String encryptedData) {}
