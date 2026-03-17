package com.ale.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ale.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Exact match on {@code first_name} scoped to a tenant. */
    List<User> findByFirstNameAndTenantId(String firstName, UUID tenantId);

    /** Exact match on {@code last_name} scoped to a tenant. */
    List<User> findByLastNameAndTenantId(String lastName, UUID tenantId);

    /**
     * Prefix match: {@code WHERE first_name LIKE 'prefix%' AND tenant_id = ?}.
     */
    List<User> findByFirstNameStartingWithAndTenantId(String prefix, UUID tenantId);

    /** Prefix match: {@code WHERE last_name LIKE 'prefix%' AND tenant_id = ?}. */
    List<User> findByLastNameStartingWithAndTenantId(String prefix, UUID tenantId);
}
