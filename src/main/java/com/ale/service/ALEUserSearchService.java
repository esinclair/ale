package com.ale.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ale.encryption.EncryptionService;
import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;

/**
 * Search service for the encrypted {@link ALEUser} entity.
 *
 * <p>Because every field in {@code ALEUser} is AES-256/GCM encrypted at rest, exact-match
 * searches are performed via the HMAC-SHA-256 blind-index columns that the
 * {@link com.ale.encryption.ALEUserEntityListener} maintains automatically on every insert/update.
 *
 * <h3>Search protocol</h3>
 * <ol>
 *   <li>Caller supplies a plaintext search term (e.g. {@code "Smith"}).</li>
 *   <li>This service computes {@code HMAC-SHA-256(searchTerm)} using the same static key that
 *       was used when the row was written.</li>
 *   <li>The resulting digest is compared against the appropriate {@code *_hash} column.</li>
 * </ol>
 */
@Service
public class ALEUserSearchService {

    private final ALEUserRepository aleUserRepository;
    private final EncryptionService encryptionService;

    public ALEUserSearchService(ALEUserRepository aleUserRepository,
                                EncryptionService encryptionService) {
        this.aleUserRepository = aleUserRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Exact first-name search via HMAC blind index.
     */
    public List<ALEUser> findByFirstName(String firstName) {
        try {
            return aleUserRepository.findByFirstNameHash(encryptionService.hash(firstName));
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash firstName search term", e);
        }
    }

    /**
     * Exact last-name search via HMAC blind index.
     */
    public List<ALEUser> findByLastName(String lastName) {
        try {
            return aleUserRepository.findByLastNameHash(encryptionService.hash(lastName));
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash lastName search term", e);
        }
    }

    /**
     * Prefix first-name search: finds all users whose {@code firstName} starts with the
     * supplied 3-character prefix.  Hashes the prefix and queries the
     * {@code first_name_prefix_hash} blind-index column — no table scan involved.
     *
     * @param prefix exactly 3 characters (trailing chars are silently truncated to 3)
     */
    public List<ALEUser> findByFirstNameStartingWith(String prefix) {
        try {
            String p = prefix.length() <= 3 ? prefix : prefix.substring(0, 3);
            return aleUserRepository.findByFirstNamePrefixHash(encryptionService.hash(p));
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash firstName prefix search term", e);
        }
    }

    /**
     * Prefix last-name search: finds all users whose {@code lastName} starts with the
     * supplied 3-character prefix via the {@code last_name_prefix_hash} blind-index column.
     *
     * @param prefix exactly 3 characters (trailing chars are silently truncated to 3)
     */
    public List<ALEUser> findByLastNameStartingWith(String prefix) {
        try {
            String p = prefix.length() <= 3 ? prefix : prefix.substring(0, 3);
            return aleUserRepository.findByLastNamePrefixHash(encryptionService.hash(p));
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash lastName prefix search term", e);
        }
    }
}
