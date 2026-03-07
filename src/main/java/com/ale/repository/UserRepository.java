package com.ale.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ale.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Exact match on {@code first_name}. Uses the {@code idx_users_first_name} btree index. */
    List<User> findByFirstName(String firstName);

    /** Exact match on {@code last_name}. Uses the {@code idx_users_last_name} btree index. */
    List<User> findByLastName(String lastName);

    /**
     * Prefix match: {@code WHERE first_name LIKE 'prefix%'}.
     * PostgreSQL can use the btree index when the collation is C or with
     * {@code varchar_pattern_ops}; otherwise falls back to a seq-scan over the indexed pages.
     */
    List<User> findByFirstNameStartingWith(String prefix);

    /** Prefix match: {@code WHERE last_name LIKE 'prefix%'}. */
    List<User> findByLastNameStartingWith(String prefix);
}
