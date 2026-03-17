package com.ale.model;

import com.ale.encryption.ALEUserEntityListener;
import com.ale.encryption.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA entity stored in the {@code aleuser} table.
 *
 * <p>Every string field (firstName, lastName, userName, email) is transparently encrypted at
 * rest via {@link EncryptedStringConverter}.  The converter uses envelope encryption:
 * each field value is encrypted with AES-256/GCM using a DEK that is itself RSA-wrapped.
 * Both the wrapped DEK and the ciphertext are stored together in a single {@code TEXT} column,
 * separated by {@code |}.  The same DEK is reused for up to 1 000 encryption calls (configurable
 * via {@code EncryptedStringConverter#DEK_REUSE_LIMIT}) before being automatically rotated.
 *
 * <p>Btree indexes are declared on all hash columns so that blind-index lookups (exact and
 * prefix) execute as single index scans rather than full-table scans.
 */
@Entity
@Table(name = "aleuser", indexes = {
    @Index(name = "idx_aleuser_tenant_id",              columnList = "tenant_id"),
    @Index(name = "idx_aleuser_first_name_hash",        columnList = "first_name_hash"),
    @Index(name = "idx_aleuser_first_name_prefix_hash", columnList = "first_name_prefix_hash"),
    @Index(name = "idx_aleuser_last_name_hash",         columnList = "last_name_hash"),
    @Index(name = "idx_aleuser_last_name_prefix_hash",  columnList = "last_name_prefix_hash"),
    @Index(name = "idx_aleuser_user_name_hash",         columnList = "user_name_hash"),
    @Index(name = "idx_aleuser_email_hash",             columnList = "email_hash")
})
@EntityListeners(ALEUserEntityListener.class)
public class ALEUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Identifies the tenant that owns this record; drives per-tenant DEK selection. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "first_name", nullable = false, columnDefinition = "TEXT")
    private String firstName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "last_name", nullable = false, columnDefinition = "TEXT")
    private String lastName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "user_name", nullable = false, unique = true, columnDefinition = "TEXT")
    private String userName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email", nullable = false, unique = true, columnDefinition = "TEXT")
    private String email;

    // ── Searchable HMAC-SHA-256 hashes (not encrypted) ────────────────────────

    @Column(name = "first_name_hash", length = 64)
    private String firstNameHash;

    @Column(name = "first_name_prefix_hash", length = 64)
    private String firstNamePrefixHash;

    @Column(name = "last_name_hash", length = 64)
    private String lastNameHash;

    @Column(name = "last_name_prefix_hash", length = 64)
    private String lastNamePrefixHash;

    @Column(name = "user_name_hash", length = 64)
    private String userNameHash;

    @Column(name = "user_name_prefix_hash", length = 64)
    private String userNamePrefixHash;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Column(name = "email_prefix_hash", length = 64)
    private String emailPrefixHash;

    public ALEUser() {}

    public ALEUser(String firstName, String lastName, String userName, String email, UUID tenantId) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.userName  = userName;
        this.email     = email;
        this.tenantId  = tenantId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // ── Hash getters/setters ──────────────────────────────────────────────────

    public String getFirstNameHash() { return firstNameHash; }
    public void setFirstNameHash(String firstNameHash) { this.firstNameHash = firstNameHash; }

    public String getFirstNamePrefixHash() { return firstNamePrefixHash; }
    public void setFirstNamePrefixHash(String firstNamePrefixHash) { this.firstNamePrefixHash = firstNamePrefixHash; }

    public String getLastNameHash() { return lastNameHash; }
    public void setLastNameHash(String lastNameHash) { this.lastNameHash = lastNameHash; }

    public String getLastNamePrefixHash() { return lastNamePrefixHash; }
    public void setLastNamePrefixHash(String lastNamePrefixHash) { this.lastNamePrefixHash = lastNamePrefixHash; }

    public String getUserNameHash() { return userNameHash; }
    public void setUserNameHash(String userNameHash) { this.userNameHash = userNameHash; }

    public String getUserNamePrefixHash() { return userNamePrefixHash; }
    public void setUserNamePrefixHash(String userNamePrefixHash) { this.userNamePrefixHash = userNamePrefixHash; }

    public String getEmailHash() { return emailHash; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }

    public String getEmailPrefixHash() { return emailPrefixHash; }
    public void setEmailPrefixHash(String emailPrefixHash) { this.emailPrefixHash = emailPrefixHash; }
}
