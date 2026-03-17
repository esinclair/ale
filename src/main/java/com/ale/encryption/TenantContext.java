package com.ale.encryption;

import java.util.UUID;

/**
 * Thread-local holder for the current request's tenant identifier.
 *
 * <p>{@link EncryptedStringConverter} reads this value during
 * {@code convertToDatabaseColumn} to select (or create) the per-tenant
 * Data Encryption Key.  Callers that trigger JPA writes (controllers,
 * services, tests) are responsible for calling {@link #set(UUID)} before
 * invoking the repository and {@link #clear()} in a {@code finally} block
 * afterwards.
 *
 * <h3>Usage pattern</h3>
 * <pre>{@code
 * TenantContext.set(tenantId);
 * try {
 *     repository.save(entity);
 * } finally {
 *     TenantContext.clear();
 * }
 * }</pre>
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    /** Sets the tenant ID for the current thread. */
    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    /**
     * Returns the tenant ID bound to the current thread, or {@code null} if none was set.
     */
    public static UUID get() {
        return CURRENT.get();
    }

    /** Removes the tenant ID from the current thread — always call this in a {@code finally} block. */
    public static void clear() {
        CURRENT.remove();
    }
}

