package com.ale.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ale.model.ALEUser;

@Repository
public interface ALEUserRepository extends JpaRepository<ALEUser, UUID> {

    /**
     * Exact first-name match via blind index.
     * Caller must supply {@code HMAC-SHA-256(plaintext)} — see
     * {@link com.ale.encryption.EncryptionService#hash}.
     */
    List<ALEUser> findByFirstNameHash(String firstNameHash);

    /**
     * Exact last-name match via blind index.
     * Caller must supply {@code HMAC-SHA-256(plaintext)}.
     */
    List<ALEUser> findByLastNameHash(String lastNameHash);

    /**
     * Prefix first-name match via the 3-char prefix blind index.
     * Caller must supply {@code HMAC-SHA-256(first3Chars)}.
     */
    List<ALEUser> findByFirstNamePrefixHash(String firstNamePrefixHash);

    /**
     * Prefix last-name match via the 3-char prefix blind index.
     * Caller must supply {@code HMAC-SHA-256(first3Chars)}.
     */
    List<ALEUser> findByLastNamePrefixHash(String lastNamePrefixHash);
}
