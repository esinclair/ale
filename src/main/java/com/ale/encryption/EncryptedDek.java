package com.ale.encryption;

import javax.crypto.SecretKey;

/**
 * Holds a freshly-generated AES-256 DEK together with its RSA-wrapped form.
 *
 * <ul>
 *   <li>{@code wrappedKey} – the DEK wrapped with the RSA public key (Base64-encoded)</li>
 *   <li>{@code rawDek}     – the unwrapped SecretKey used directly for AES/GCM encryption</li>
 * </ul>
 */
public record EncryptedDek(String wrappedKey, SecretKey rawDek) {}
