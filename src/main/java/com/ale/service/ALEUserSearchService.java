package com.ale.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ale.encryption.EncryptionService;
import com.ale.model.ALEUser;
import com.ale.repository.ALEUserRepository;

/**
 * Search service for the encrypted {@link ALEUser} entity.
 *
 * <p>All search operations are scoped to a single {@code tenantId}.  The supplied plaintext
 * search term is hashed with {@link EncryptionService#hash(String, UUID)} (which prefixes the
 * HMAC input with the tenant UUID) before being compared against the appropriate blind-index
 * column.
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

    /** Exact first-name search via HMAC blind index, scoped to {@code tenantId}. */
    public List<ALEUser> findByFirstName(String firstName, UUID tenantId) {
        try {
            return aleUserRepository.findByFirstNameHashAndTenantId(
                    encryptionService.hash(firstName), tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash firstName search term", e);
        }
    }

    /** Exact last-name search via HMAC blind index, scoped to {@code tenantId}. */
    public List<ALEUser> findByLastName(String lastName, UUID tenantId) {
        try {
            return aleUserRepository.findByLastNameHashAndTenantId(
                    encryptionService.hash(lastName), tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash lastName search term", e);
        }
    }

    /**
     * Prefix first-name search (3-char) via the {@code first_name_prefix_hash} blind-index,
     * scoped to {@code tenantId}.
     *
     * @param prefix exactly 3 characters (trailing chars are silently truncated to 3)
     */
    public List<ALEUser> findByFirstNameStartingWith(String prefix, UUID tenantId) {
        try {
            String p = prefix.length() <= 3 ? prefix : prefix.substring(0, 3);
            return aleUserRepository.findByFirstNamePrefixHashAndTenantId(
                    encryptionService.hash(p), tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash firstName prefix search term", e);
        }
    }

    /**
     * Prefix last-name search (3-char) via the {@code last_name_prefix_hash} blind-index,
     * scoped to {@code tenantId}.
     *
     * @param prefix exactly 3 characters (trailing chars are silently truncated to 3)
     */
    public List<ALEUser> findByLastNameStartingWith(String prefix, UUID tenantId) {
        try {
            String p = prefix.length() <= 3 ? prefix : prefix.substring(0, 3);
            return aleUserRepository.findByLastNamePrefixHashAndTenantId(
                    encryptionService.hash(p), tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash lastName prefix search term", e);
        }
    }
}
