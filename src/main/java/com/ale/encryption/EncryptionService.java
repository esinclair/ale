package com.ale.encryption;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Envelope encryption service.
 *
 * <p>Each call to {@link #encrypt(String)} generates a fresh AES-256 Data Encryption Key (DEK).
 * The plaintext is encrypted with AES/GCM/NoPadding using the DEK, and the DEK itself is
 * wrapped with the RSA-2048 public key (Key Encryption Key, KEK).  Both the wrapped DEK and
 * the ciphertext are returned in an {@link EncryptedPayload}.
 *
 * <p>Decryption reverses the process: the wrapped DEK is unwrapped with the RSA private key
 * and the ciphertext is decrypted with the recovered DEK.
 *
 * <p>Algorithm choices:
 * <ul>
 *   <li>DEK: AES-256</li>
 *   <li>Data cipher: AES/GCM/NoPadding (128-bit tag, 12-byte IV)</li>
 *   <li>Key wrap: RSA/ECB/OAEPWithSHA-256AndMGF1Padding</li>
 * </ul>
 */
@Service
public class EncryptionService {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String RSA_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final int AES_KEY_BITS = 256;

    @Value("${encryption.rsa.public-key}")
    private String publicKeyBase64;

    @Value("${encryption.rsa.private-key}")
    private String privateKeyBase64;

    @Value("${encryption.hmac.key}")
    private String hmacKeyValue;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Mac hmac;

    @PostConstruct
    void init() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
        privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));

        SecretKeySpec hmacKeySpec = new SecretKeySpec(hmacKeyValue.getBytes("UTF-8"), "HmacSHA256");
        hmac = Mac.getInstance("HmacSHA256");
        hmac.init(hmacKeySpec);
    }

    /**
     * Encrypts {@code plaintext} using envelope encryption.
     *
     * @param plaintext UTF-8 text to encrypt
     * @return an {@link EncryptedPayload} containing the RSA-wrapped DEK and the AES-GCM ciphertext
     */
    public EncryptedPayload encrypt(String plaintext) throws Exception {
        // 1. Generate a fresh AES-256 DEK
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_BITS, new SecureRandom());
        SecretKey dek = keyGen.generateKey();

        // 2. Encrypt plaintext with AES/GCM; prepend 12-byte IV to ciphertext
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance(AES_GCM);
        aesCipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes("UTF-8"));

        byte[] ivAndCiphertext = new byte[GCM_IV_BYTES + ciphertext.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, GCM_IV_BYTES);
        System.arraycopy(ciphertext, 0, ivAndCiphertext, GCM_IV_BYTES, ciphertext.length);

        // 3. Wrap the DEK with the RSA public key (OAEP-SHA-256)
        Cipher rsaCipher = Cipher.getInstance(RSA_OAEP);
        rsaCipher.init(Cipher.WRAP_MODE, publicKey, oaepParams());
        byte[] wrappedKey = rsaCipher.wrap(dek);

        return new EncryptedPayload(
                Base64.getEncoder().encodeToString(wrappedKey),
                Base64.getEncoder().encodeToString(ivAndCiphertext));
    }

    /**
     * Decrypts an {@link EncryptedPayload} produced by {@link #encrypt(String)}.
     *
     * @param payload the encrypted payload
     * @return the original plaintext
     */
    public String decrypt(EncryptedPayload payload) throws Exception {
        // 1. Unwrap the DEK with the RSA private key
        Cipher rsaCipher = Cipher.getInstance(RSA_OAEP);
        rsaCipher.init(Cipher.UNWRAP_MODE, privateKey, oaepParams());
        SecretKey dek = (SecretKey) rsaCipher.unwrap(
                Base64.getDecoder().decode(payload.encryptedKey()), "AES", Cipher.SECRET_KEY);

        // 2. Decrypt the ciphertext; extract IV from the first 12 bytes
        byte[] ivAndCiphertext = Base64.getDecoder().decode(payload.encryptedData());
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, ivAndCiphertext, 0, GCM_IV_BYTES);
        Cipher aesCipher = Cipher.getInstance(AES_GCM);
        aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek.getEncoded(), "AES"), spec);
        byte[] plaintext = aesCipher.doFinal(ivAndCiphertext, GCM_IV_BYTES, ivAndCiphertext.length - GCM_IV_BYTES);
        return new String(plaintext, "UTF-8");
    }

    /**
     * Computes an HMAC-SHA-256 of {@code value} using the static key from
     * {@code encryption.hmac.key}, returning a lowercase hex string.
     *
     * <p>This is safe for concurrent use – each call clones the pre-initialised
     * {@link Mac} rather than re-using a shared mutable instance.
     *
     * @param value the plaintext value to hash (null returns null)
     * @return 64-character lowercase hex HMAC digest, or {@code null} for null input
     */
    public String hash(String value) throws Exception {
        if (value == null) return null;
        Mac localMac = (Mac) hmac.clone();
        byte[] digest = localMac.doFinal(value.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Generates a fresh AES-256 DEK and wraps it with the RSA public key.
     * The returned {@link EncryptedDek} holds both the Base64-encoded wrapped key (for storage)
     * and the raw {@link SecretKey} (for immediate use without an extra unwrap step).
     */
    public EncryptedDek generateAndWrapDek() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_BITS, new SecureRandom());
        SecretKey dek = keyGen.generateKey();

        Cipher rsaCipher = Cipher.getInstance(RSA_OAEP);
        rsaCipher.init(Cipher.WRAP_MODE, publicKey, oaepParams());
        byte[] wrappedKey = rsaCipher.wrap(dek);

        return new EncryptedDek(Base64.getEncoder().encodeToString(wrappedKey), dek);
    }

    /**
     * Encrypts {@code plaintext} with a caller-supplied DEK using AES/GCM/NoPadding.
     * A fresh 12-byte IV is prepended to the ciphertext.
     *
     * @param plaintext UTF-8 text to encrypt
     * @param dek       an AES-256 key previously obtained from {@link #generateAndWrapDek()}
     * @return Base64-encoded IV + ciphertext
     */
    public String encryptWithDek(String plaintext, SecretKey dek) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance(AES_GCM);
        aesCipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = aesCipher.doFinal(plaintext.getBytes("UTF-8"));

        byte[] ivAndCiphertext = new byte[GCM_IV_BYTES + ciphertext.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, GCM_IV_BYTES);
        System.arraycopy(ciphertext, 0, ivAndCiphertext, GCM_IV_BYTES, ciphertext.length);
        return Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    /**
     * Convenience method for decrypting a field stored by {@link com.ale.encryption.EncryptedStringConverter}.
     *
     * @param wrappedKey    Base64-encoded RSA-wrapped DEK
     * @param encryptedData Base64-encoded IV + ciphertext
     * @return the original plaintext
     */
    public String decryptField(String wrappedKey, String encryptedData) throws Exception {
        return decrypt(new EncryptedPayload(wrappedKey, encryptedData));
    }

    private static OAEPParameterSpec oaepParams() {
        return new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    }
}
