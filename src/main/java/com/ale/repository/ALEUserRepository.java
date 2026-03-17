package com.ale.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ale.model.ALEUser;

@Repository
public interface ALEUserRepository extends JpaRepository<ALEUser, UUID> {

    /**
     * Exact first-name match scoped to a tenant via the blind index.
     * Caller must supply {@code HMAC-SHA-256(tenantId + ":" + plaintext)} — see
     * {@link com.ale.encryption.EncryptionService#hash(String, java.util.UUID)}.
     */
    List<ALEUser> findByFirstNameHashAndTenantId(String firstNameHash, UUID tenantId);

    /**
     * Exact last-name match scoped to a tenant via the blind index.
     */
    List<ALEUser> findByLastNameHashAndTenantId(String lastNameHash, UUID tenantId);

    /**
     * Prefix first-name match (3-char) scoped to a tenant via the prefix blind index.
     */
    List<ALEUser> findByFirstNamePrefixHashAndTenantId(String firstNamePrefixHash, UUID tenantId);

    /**
     * Prefix last-name match (3-char) scoped to a tenant via the prefix blind index.
     */
    List<ALEUser> findByLastNamePrefixHashAndTenantId(String lastNamePrefixHash, UUID tenantId);
}
