package com.ale.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ale.model.User;
import com.ale.repository.UserRepository;

/**
 * Search service for the plaintext {@link User} entity.
 *
 * <p>All search operations are scoped to a single {@code tenantId} so that
 * results from one tenant are never visible to another.
 */
@Service
public class UserSearchService {

    private final UserRepository userRepository;

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Exact {@code firstName} match scoped to a tenant. */
    public List<User> findByFirstName(String firstName, UUID tenantId) {
        return userRepository.findByFirstNameAndTenantId(firstName, tenantId);
    }

    /** Exact {@code lastName} match scoped to a tenant. */
    public List<User> findByLastName(String lastName, UUID tenantId) {
        return userRepository.findByLastNameAndTenantId(lastName, tenantId);
    }

    /** Prefix {@code firstName} match scoped to a tenant. */
    public List<User> findByFirstNameStartingWith(String prefix, UUID tenantId) {
        return userRepository.findByFirstNameStartingWithAndTenantId(prefix, tenantId);
    }

    /** Prefix {@code lastName} match scoped to a tenant. */
    public List<User> findByLastNameStartingWith(String prefix, UUID tenantId) {
        return userRepository.findByLastNameStartingWithAndTenantId(prefix, tenantId);
    }
}
