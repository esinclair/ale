package com.ale.encryption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ale.model.ALEUser;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA entity listener that computes HMAC-SHA-256 hashes for all {@link ALEUser} string fields
 * immediately before insert ({@link PrePersist}) and update ({@link PreUpdate}).
 *
 * <h3>Hashes stored</h3>
 * For each of the four fields ({@code firstName}, {@code lastName}, {@code userName},
 * {@code email}) two hashes are computed:
 * <ul>
 *   <li><b>full hash</b>  – HMAC of the complete plaintext value</li>
 *   <li><b>prefix hash</b> – HMAC of the first three characters (for prefix-search use-cases)</li>
 * </ul>
 * Hashes are computed with the global (non-tenant-scoped) {@link EncryptionService#hash(String)}
 * method using a single shared HMAC key.  Tenant isolation for searches is enforced at the
 * query level via the {@code tenant_id} column filter in the repository.
 *
 * <h3>Spring integration</h3>
 * JPA instantiates entity listeners via {@code new} (outside the Spring container), so
 * direct {@code @Autowired} on instance fields would not work.  Instead, Spring initialises
 * this class as a singleton bean and populates a {@code static} reference to
 * {@link EncryptionService}; all JPA-created instances then share that reference.
 */
@Component
public class ALEUserEntityListener {

    /** Populated by Spring; accessible to JPA-created listener instances via static reference. */
    private static EncryptionService encryptionService;

    @Autowired
    void inject(EncryptionService service) {
        ALEUserEntityListener.encryptionService = service;
    }

    @PrePersist
    @PreUpdate
    public void computeHashes(Object entity) {
        if (!(entity instanceof ALEUser user)) return;
        if (encryptionService == null) return;

        try {
            user.setFirstNameHash(encryptionService.hash(user.getFirstName()));
            user.setFirstNamePrefixHash(encryptionService.hash(prefix3(user.getFirstName())));

            user.setLastNameHash(encryptionService.hash(user.getLastName()));
            user.setLastNamePrefixHash(encryptionService.hash(prefix3(user.getLastName())));

            user.setUserNameHash(encryptionService.hash(user.getUserName()));
            user.setUserNamePrefixHash(encryptionService.hash(prefix3(user.getUserName())));

            user.setEmailHash(encryptionService.hash(user.getEmail()));
            user.setEmailPrefixHash(encryptionService.hash(prefix3(user.getEmail())));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute field hashes for ALEUser", e);
        }
    }

    /** Returns the first {@code n} characters of {@code value}, or the full value if shorter. */
    private static String prefix3(String value) {
        if (value == null) return null;
        return value.length() <= 3 ? value : value.substring(0, 3);
    }
}
